package com.racephotos.service.storage;

import com.racephotos.domain.photo.PhotoAssetRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AdminStorageService {

    private static final Logger log = LogManager.getLogger(AdminStorageService.class);
    private static final int MAX_DELETE_BATCH = 1000;

    private final S3Client s3Client;
    private final PhotoAssetRepository photoAssetRepository;
    private final String bucket;

    public AdminStorageService(
            S3Client s3Client,
            PhotoAssetRepository photoAssetRepository,
            @Value("${aws.s3.bucket:}") String bucket
    ) {
        this.s3Client = Objects.requireNonNull(s3Client, "s3Client");
        this.photoAssetRepository = Objects.requireNonNull(photoAssetRepository, "photoAssetRepository");
        this.bucket = bucket;
    }

    public DeleteByPrefixResult deleteByPrefix(String prefix) {
        String sanitizedPrefix = sanitizePrefix(prefix);
        ensureBucketConfigured();

        long deletedS3Objects = deleteS3Objects(sanitizedPrefix);
        long deletedAssets = photoAssetRepository.deleteByObjectKeyStartingWith(sanitizedPrefix);

        log.info("Deleted {} S3 objects and {} photo assets with prefix '{}'", deletedS3Objects, deletedAssets, sanitizedPrefix);
        return new DeleteByPrefixResult(deletedS3Objects, deletedAssets);
    }

    private long deleteS3Objects(String prefix) {
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();
        try {
            ListObjectsV2Iterable pages = s3Client.listObjectsV2Paginator(listRequest);

            long deleted = 0L;
            List<ObjectIdentifier> batch = new ArrayList<>(MAX_DELETE_BATCH);

            for (ListObjectsV2Response page : pages) {
                if (page.contents() == null) {
                    continue;
                }
                for (S3Object obj : page.contents()) {
                    if (obj == null || obj.key() == null || obj.key().isBlank()) {
                        continue;
                    }
                    batch.add(ObjectIdentifier.builder().key(obj.key()).build());
                    if (batch.size() == MAX_DELETE_BATCH) {
                        deleted += sendBatchDelete(prefix, batch);
                        batch.clear();
                    }
                }
            }

            if (!batch.isEmpty()) {
                deleted += sendBatchDelete(prefix, batch);
            }

            return deleted;
        } catch (SdkException e) {
            log.error("Failed to delete S3 objects for prefix {}", prefix, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete S3 objects", e);
        }
    }

    private long sendBatchDelete(String prefix, List<ObjectIdentifier> objects) {
        DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(objects).build())
                .build();
        try {
            var result = s3Client.deleteObjects(request);
            if (result.errors() != null && !result.errors().isEmpty()) {
                log.error("S3 deleteObjects returned errors for prefix {}: {}", prefix, result.errors());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete some S3 objects");
            }
            return result.deleted() == null ? 0L : result.deleted().size();
        } catch (SdkException e) {
            log.error("Failed to delete S3 objects for prefix {} ({} keys)", prefix, objects.size(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete S3 objects", e);
        }
    }

    private void ensureBucketConfigured() {
        if (bucket == null || bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 bucket not configured");
        }
    }

    private String sanitizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prefix is required");
        }
        String sanitized = S3StringUtils.sanitizeOptionalFolder(prefix);
        if (sanitized == null || sanitized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prefix is required");
        }
        return sanitized;
    }

    public record DeleteByPrefixResult(long deletedS3Objects, long deletedPhotoAssets) { }
}
