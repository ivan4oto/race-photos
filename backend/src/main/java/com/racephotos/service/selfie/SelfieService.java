package com.racephotos.service.selfie;

import com.racephotos.auth.session.SessionUser;
import com.racephotos.auth.user.User;
import com.racephotos.auth.user.UserRepository;
import com.racephotos.auth.user.UserSelfie;
import com.racephotos.auth.user.UserSelfieRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class SelfieService {

    private static final Logger log = LogManager.getLogger(SelfieService.class);
    private static final long MAX_SELFIE_BYTES = 4L * 1024 * 1024;
    private static final float SIMILARITY_THRESHOLD = 90.0f;
    private static final int MAX_UPLOADS = 5;

    private final RekognitionClient rekognitionClient;
    private final S3Client s3Client;
    private final UserRepository userRepository;
    private final UserSelfieRepository selfieRepository;
    private final String bucket;
    private final String selfieCollectionId;
    private final Set<String> ensuredCollections = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public SelfieService(
            RekognitionClient rekognitionClient,
            S3Client s3Client,
            UserRepository userRepository,
            UserSelfieRepository selfieRepository,
            @Value("${aws.s3.bucket:}") String bucket,
            @Value("${aws.rekognition.selfie-collection-id:selfies}") String selfieCollectionId
    ) {
        this.rekognitionClient = Objects.requireNonNull(rekognitionClient, "rekognitionClient");
        this.s3Client = Objects.requireNonNull(s3Client, "s3Client");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.selfieRepository = Objects.requireNonNull(selfieRepository, "selfieRepository");
        this.bucket = bucket;
        this.selfieCollectionId = selfieCollectionId;
    }

    @Transactional
    public void uploadSelfie(SessionUser sessionUser, MultipartFile file) {
        if (sessionUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findById(sessionUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session invalid"));

        if (bucket == null || bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 bucket not configured");
        }

        validateFile(file);
        ensureCollectionExists();

        UserSelfie existing = selfieRepository.findByUserId(user.getId()).orElse(null);
        if (existing != null && existing.getUploadCount() >= MAX_UPLOADS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Maximum selfie uploads reached");
        }

        String key = buildSelfieKey(user.getId(), file.getOriginalFilename());

        try {
            putObject(key, file);
            IndexFacesResponse indexed = indexSelfie(key, user.getId());
            List<FaceRecord> faces = indexed.faceRecords();
            if (faces == null || faces.isEmpty()) {
                cleanupNewUpload(key, null);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No face detected in the selfie");
            }
            if (faces.size() > 1) {
                cleanupNewUpload(key, extractFaceIds(faces));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only one face is allowed in the selfie");
            }

            String newFaceId = faces.get(0).face().faceId();
            if (newFaceId == null || newFaceId.isBlank()) {
                cleanupNewUpload(key, extractFaceIds(faces));
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to index selfie face");
            }

            if (existing != null) {
                float similarity = compareFaces(existing.getS3Key(), key);
                if (similarity < SIMILARITY_THRESHOLD) {
                    cleanupNewUpload(key, List.of(newFaceId));
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selfie appears to be a different person");
                }
                deleteFace(existing.getFaceId());
                deleteObject(existing.getS3Key());
                existing.setFaceId(newFaceId);
                existing.setS3Key(key);
                existing.setUploadCount(existing.getUploadCount() + 1);
                selfieRepository.save(existing);
                log.info("Replaced selfie for user {} with faceId {} (similarity {}%)", user.getId(), newFaceId, similarity);
            } else {
                UserSelfie selfie = new UserSelfie();
                selfie.setUser(user);
                selfie.setFaceId(newFaceId);
                selfie.setS3Key(key);
                selfie.setUploadCount(1);
                selfieRepository.save(selfie);
                log.info("Saved new selfie for user {} with faceId {}", user.getId(), newFaceId);
            }
        } catch (IOException e) {
            deleteObject(key);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload selfie", e);
        }
    }

    @Transactional
    public void deleteSelfie(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id required");
        }
        UserSelfie selfie = selfieRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selfie not found for user"));

        deleteFace(selfie.getFaceId());
        deleteObject(selfie.getS3Key());
        selfieRepository.delete(selfie);
        log.info("Deleted selfie for user {} (faceId {})", userId, selfie.getFaceId());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selfie file is required");
        }
        if (file.getSize() > MAX_SELFIE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Selfie exceeds 4MB limit");
        }
    }

    private void ensureCollectionExists() {
        if (selfieCollectionId == null || selfieCollectionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Selfie collection id not configured");
        }
        if (ensuredCollections.contains(selfieCollectionId)) {
            return;
        }
        synchronized (ensuredCollections) {
            if (ensuredCollections.contains(selfieCollectionId)) {
                return;
            }
            try {
                rekognitionClient.describeCollection(DescribeCollectionRequest.builder()
                        .collectionId(selfieCollectionId)
                        .build());
                log.debug("Selfie collection '{}' already exists", selfieCollectionId);
            } catch (ResourceNotFoundException notFound) {
                log.info("Creating selfie collection '{}'", selfieCollectionId);
                rekognitionClient.createCollection(CreateCollectionRequest.builder()
                        .collectionId(selfieCollectionId)
                        .build());
            }
            ensuredCollections.add(selfieCollectionId);
        }
    }

    private void putObject(String key, MultipartFile file) throws IOException {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();
        s3Client.putObject(put, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }

    private IndexFacesResponse indexSelfie(String key, UUID userId) {
        IndexFacesRequest request = IndexFacesRequest.builder()
                .collectionId(selfieCollectionId)
                .externalImageId(userId.toString())
                .qualityFilter(QualityFilter.AUTO)
                .image(Image.builder()
                        .s3Object(S3Object.builder()
                                .bucket(bucket)
                                .name(key)
                                .build())
                        .build())
                .build();
        return rekognitionClient.indexFaces(request);
    }

    private float compareFaces(String existingKey, String newKey) {
        CompareFacesRequest request = CompareFacesRequest.builder()
                .sourceImage(imageFromKey(existingKey))
                .targetImage(imageFromKey(newKey))
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();
        CompareFacesResponse response = rekognitionClient.compareFaces(request);
        double max = response.faceMatches() == null ? 0.0 : response.faceMatches().stream()
                .map(CompareFacesMatch::similarity)
                .filter(Objects::nonNull)
                .mapToDouble(Float::doubleValue)
                .max()
                .orElse(0.0);
        return (float) max;
    }

    private Image imageFromKey(String key) {
        return Image.builder()
                .s3Object(S3Object.builder()
                        .bucket(bucket)
                        .name(key)
                        .build())
                .build();
    }

    private void deleteFace(String faceId) {
        if (faceId == null || faceId.isBlank()) {
            return;
        }
        rekognitionClient.deleteFaces(DeleteFacesRequest.builder()
                .collectionId(selfieCollectionId)
                .faceIds(faceId)
                .build());
    }

    private void deleteObject(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete S3 object {}: {}", key, e.getMessage());
        }
    }

    private void cleanupNewUpload(String key, List<String> faceIds) {
        if (faceIds != null && !faceIds.isEmpty()) {
            deleteFaces(faceIds);
        }
        deleteObject(key);
    }

    private void deleteFaces(List<String> faceIds) {
        rekognitionClient.deleteFaces(DeleteFacesRequest.builder()
                .collectionId(selfieCollectionId)
                .faceIds(faceIds)
                .build());
    }

    private List<String> extractFaceIds(List<FaceRecord> records) {
        return records.stream()
                .map(fr -> fr.face() == null ? null : fr.face().faceId())
                .filter(Objects::nonNull)
                .toList();
    }

    private String buildSelfieKey(UUID userId, String originalName) {
        String ext = ".jpg";
        if (originalName != null && originalName.contains(".")) {
            String candidate = originalName.substring(originalName.lastIndexOf('.'));
            if (candidate.length() <= 5) {
                ext = candidate;
            }
        }
        return "selfies/" + userId + "/" + UUID.randomUUID() + ext;
    }
}
