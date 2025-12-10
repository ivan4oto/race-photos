package com.racephotos.service.search;

import com.racephotos.service.storage.S3StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminFaceSearchService {

    private static final Logger log = LogManager.getLogger(AdminFaceSearchService.class);
    private static final String TEMP_PREFIX = "temp/";
    private static final long MAX_UPLOAD_BYTES = 6L * 1024 * 1024;

    private final S3Client s3Client;
    private final FaceSearchService faceSearchService;
    private final String bucket;

    public AdminFaceSearchService(
            S3Client s3Client,
            FaceSearchService faceSearchService,
            @Value("${aws.s3.bucket:}") String bucket
    ) {
        this.s3Client = Objects.requireNonNull(s3Client, "s3Client");
        this.faceSearchService = Objects.requireNonNull(faceSearchService, "faceSearchService");
        this.bucket = bucket;
    }

    public FaceSearchService.FaceSearchResult uploadProbeAndSearch(UUID eventId, MultipartFile file) {
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId is required");
        }
        ensureBucketConfigured();
        validateFile(file);

        String tempKey = buildTempKey(file.getOriginalFilename());
        putTempObject(tempKey, file);

        String latestKey = findLatestTempKey()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Uploaded selfie not found in temp/"));
        log.info("Admin face search for event {} using probe {}", eventId, latestKey);
        return faceSearchService.searchFaces(eventId.toString(), latestKey);
    }

    private void ensureBucketConfigured() {
        if (bucket == null || bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 bucket not configured");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selfie file is required");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds 6MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image uploads are allowed");
        }
    }

    private String buildTempKey(String originalName) {
        String safeName = S3StringUtils.sanitizeFilename(
                originalName == null || originalName.isBlank() ? "selfie.jpg" : originalName
        );
        return TEMP_PREFIX + UUID.randomUUID() + "-" + safeName;
    }

    private void putTempObject(String key, MultipartFile file) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();
        try {
            s3Client.putObject(put, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | SdkException e) {
            log.error("Failed to upload admin probe to {}", key, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file", e);
        }
    }

    private Optional<String> findLatestTempKey() {
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(TEMP_PREFIX)
                .build();
        try {
            ListObjectsV2Iterable pages = s3Client.listObjectsV2Paginator(listRequest);

            S3Object latest = null;
            for (var page : pages) {
                if (page.contents() == null) {
                    continue;
                }
                for (S3Object obj : page.contents()) {
                    if (obj == null || obj.key() == null || obj.key().endsWith("/")) {
                        continue;
                    }
                    if (!obj.key().startsWith(TEMP_PREFIX)) {
                        continue;
                    }
                    if (latest == null || isAfter(obj, latest)) {
                        latest = obj;
                    }
                }
            }
            return latest == null ? Optional.empty() : Optional.of(latest.key());
        } catch (SdkException e) {
            log.error("Failed to list temp objects for admin face search", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find latest temp upload", e);
        }
    }

    private boolean isAfter(S3Object candidate, S3Object baseline) {
        Instant candidateTime = candidate.lastModified();
        Instant baselineTime = baseline.lastModified();
        if (candidateTime == null) {
            return false;
        }
        if (baselineTime == null) {
            return true;
        }
        return candidateTime.isAfter(baselineTime);
    }
}
