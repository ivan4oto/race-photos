package com.racephotos.service.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3UrlService {

    private static final Logger log = LogManager.getLogger(S3UrlService.class);

    private final S3Presigner presigner;
    private final String bucket;
    private final long expirationSeconds;

    public S3UrlService(
            S3Presigner presigner,
            @Value("${aws.s3.bucket:}") String bucket,
            @Value("${aws.s3.presign.expiration-seconds:7200}") long expirationSeconds
    ) {
        this.presigner = presigner;
        this.bucket = bucket;
        this.expirationSeconds = expirationSeconds;
    }

    public List<UrlEntry> createPresignedPutUrls(List<String> names) {
        if (bucket == null || bucket.isBlank()) {
            log.error("S3 bucket not configured (aws.s3.bucket is blank)");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 bucket is not configured");
        }
        if (names == null || names.isEmpty()) {
            log.warn("Request with empty names list rejected");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Names list must not be empty");
        }
        if (names.size() > 200) {
            log.warn("Request with too many names: {} (max 200)", names.size());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many names; max 200");
        }

        log.info("Generating {} presigned PUT URLs for bucket '{}' with expiry {}s", names.size(), bucket, expirationSeconds);

        List<UrlEntry> result = new ArrayList<>(names.size());
        for (String name : names) {
            String key = validateKey(name);
            log.debug("Presigning PUT for key='{}'", key);

            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expirationSeconds))
                    .putObjectRequest(put)
                    .build();

            String url = presigner.presignPutObject(presign)
                    .url()
                    .toString();

            result.add(new UrlEntry(key, url));
        }
        log.info("Generated {} presigned URLs", result.size());
        return result;
    }

    private String validateKey(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Object name is blank");
        }
        String key = name.trim();
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        if (key.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid object name");
        }
        return key;
    }

    public record UrlEntry(String name, String url) {}
}
