package com.racephotos.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.BoundingBox;
import software.amazon.awssdk.services.rekognition.model.Face;
import software.amazon.awssdk.services.rekognition.model.FaceMatch;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageRequest;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageResponse;
import software.amazon.awssdk.services.rekognition.model.S3Object;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FaceSearchService {

    private static final Logger log = LogManager.getLogger(FaceSearchService.class);

    private final RekognitionClient rekognitionClient;
    private final FaceMetadataRepository metadataRepository;
    private final String bucket;
    private final String collectionId;
    private final Integer maxFaces;
    private final Float similarityThreshold;

    public FaceSearchService(
            RekognitionClient rekognitionClient,
            FaceMetadataRepository metadataRepository,
            @Value("${aws.s3.bucket:}") String bucket,
            @Value("${aws.rekognition.collection-id:}") String collectionId,
            @Value("${aws.rekognition.search.max-faces}") Integer maxFaces,
            @Value("${aws.rekognition.search.threshold}") Float similarityThreshold
    ) {
        this.rekognitionClient = Objects.requireNonNull(rekognitionClient, "rekognitionClient");
        this.metadataRepository = Objects.requireNonNull(metadataRepository, "metadataRepository");
        this.bucket = bucket;
        this.collectionId = collectionId;
        this.maxFaces = maxFaces;
        this.similarityThreshold = similarityThreshold;
    }

    public FaceSearchResult searchFaces(String eventId, String photoKey) {
        long startNanos = System.nanoTime();
        log.debug("Starting face search for event {} and probe key {}", eventId, photoKey);
        String normalizedKey = validateInputs(eventId, photoKey);
        SearchFacesByImageResponse response = runSearch(normalizedKey);

        Map<String, Match> matchesByPhoto = new LinkedHashMap<>();
        List<FaceMatch> faceMatches = response.faceMatches();
        if (faceMatches != null) {
            for (FaceMatch faceMatch : faceMatches) {
                processMatch(eventId, normalizedKey, faceMatch, matchesByPhoto);
            }
        }

        FaceSearchResult result = new FaceSearchResult(eventId, normalizedKey, List.copyOf(matchesByPhoto.values()));
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        log.debug(
                "Finished face search for event {} key {} -> {} aggregated matches ({} raw Rekognition matches) in {} ms",
                eventId,
                normalizedKey,
                result.matches().size(),
                faceMatches == null ? 0 : faceMatches.size(),
                elapsedMillis
        );
        return result;
    }

    private void processMatch(String eventId, String probeKey, FaceMatch faceMatch, Map<String, Match> matchesByPhoto) {
        if (faceMatch == null || faceMatch.face() == null) {
            return;
        }
        Face face = faceMatch.face();
        metadataRepository.findByFaceId(face.faceId()).ifPresent(metadata -> {
            if (!eventId.equals(metadata.eventId())) {
                log.debug("Skipping face {} because event {} != {}", face.faceId(), metadata.eventId(), eventId);
                return;
            }
            if (probeKey.equals(metadata.photoKey())) {
                log.debug("Skipping face {} because {} matches the probe image", face.faceId(), metadata.photoKey());
                return; // exclude the probe image itself
            }

            matchesByPhoto.compute(metadata.photoKey(), (key, existing) -> {
                float similarity = faceMatch.similarity() == null ? 0F : faceMatch.similarity();
                Match candidate = new Match(
                        metadata.photoKey(),
                        face.faceId(),
                        similarity,
                        face.confidence(),
                        metadata.boundingBox()
                );
                if (existing == null || similarity > existing.similarity()) {
                    log.trace(
                            "Updated match for photo {} with face {} (similarity={}, confidence={})",
                            metadata.photoKey(),
                            face.faceId(),
                            similarity,
                            face.confidence()
                    );
                    return candidate;
                }
                return existing;
            });
        });
    }

    private SearchFacesByImageResponse runSearch(String photoKey) {
        try {
            SearchFacesByImageRequest.Builder builder = SearchFacesByImageRequest.builder()
                    .collectionId(collectionId)
                    .image(Image.builder()
                            .s3Object(S3Object.builder()
                                    .bucket(bucket)
                                    .name(photoKey)
                                    .build())
                            .build());

            if (maxFaces != null) {
                builder.maxFaces(maxFaces);
            }
            if (similarityThreshold != null) {
                builder.faceMatchThreshold(similarityThreshold);
            }

            log.debug(
                    "Invoking Rekognition searchFacesByImage for bucket {} key {} collection {} maxFaces={} threshold={}",
                    bucket,
                    photoKey,
                    collectionId,
                    maxFaces,
                    similarityThreshold
            );
            return rekognitionClient.searchFacesByImage(builder.build());
        } catch (SdkException e) {
            log.error("searchFacesByImage failed for key {}", photoKey, e);
            throw e;
        }
    }

    private String validateInputs(String eventId, String photoKey) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 bucket name (aws.s3.bucket) is not configured");
        }
        if (collectionId == null || collectionId.isBlank()) {
            throw new IllegalStateException("Rekognition collection id (aws.rekognition.collection-id) is not configured");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        if (photoKey == null || photoKey.isBlank()) {
            throw new IllegalArgumentException("photoKey must not be blank");
        }
        return photoKey.trim();
    }

    public record FaceSearchResult(String eventId, String probePhotoKey, List<Match> matches) {}

    public record Match(String photoKey, String faceId, float similarity, Float confidence, BoundingBox boundingBox) {}
}
