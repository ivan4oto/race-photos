package com.racephotos.service.ingestion;

import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventRepository;
import com.racephotos.domain.photo.PhotoAsset;
import com.racephotos.domain.photo.PhotoAssetRepository;
import com.racephotos.domain.photographer.Photographer;
import com.racephotos.domain.photographer.PhotographerRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class PhotoAssetIngestionService {

    private static final Logger log = LogManager.getLogger(PhotoAssetIngestionService.class);
    private static final List<String> CAPTURE_METADATA_KEYS = List.of(
            "captured-at",
            "capture-timestamp",
            "captured_at",
            "capture_time",
            "datetimeoriginal"
    );

    private final S3Client s3Client;
    private final PhotoAssetRepository photoAssetRepository;
    private final EventRepository eventRepository;
    private final PhotographerRepository photographerRepository;
    private final String defaultBucket;

    public PhotoAssetIngestionService(
            S3Client s3Client,
            PhotoAssetRepository photoAssetRepository,
            EventRepository eventRepository,
            PhotographerRepository photographerRepository,
            @Value("${aws.s3.bucket:}") String defaultBucket
    ) {
        this.s3Client = Objects.requireNonNull(s3Client, "s3Client");
        this.photoAssetRepository = Objects.requireNonNull(photoAssetRepository, "photoAssetRepository");
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        this.photographerRepository = Objects.requireNonNull(photographerRepository, "photographerRepository");
        this.defaultBucket = defaultBucket;
    }

    public IngestionResult ingestPhotoUpload(PhotoUploadNotification notification) {
        validateNotification(notification);

        String bucket = resolveBucket(notification.bucket());
        Event event = eventRepository.findById(notification.eventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + notification.eventId()));
        Photographer photographer = photographerRepository.findById(notification.photographerId())
                .orElseThrow(() -> new IllegalArgumentException("Photographer not found: " + notification.photographerId()));

        List<PhotoAsset> toPersist = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (String rawKey : notification.objectKeys()) {
            if (rawKey == null || rawKey.isBlank()) {
                failures.add("<blank>");
                continue;
            }
            String key = normalizeKey(rawKey);

            if (photoAssetRepository.existsByBucketAndObjectKey(bucket, key)) {
                skipped.add(key);
                continue;
            }

            HeadObjectResponse head;
            try {
                head = fetchObjectMetadata(bucket, key);
            } catch (SdkException sdkEx) {
                log.error("Failed to read metadata for bucket={} key={}: {}", bucket, key, sdkEx.getMessage());
                failures.add(key);
                continue;
            }

            PhotoAsset asset = buildPhotoAsset(bucket, key, event, photographer, head);
            toPersist.add(asset);
        }

        List<PhotoAsset> saved = toPersist.isEmpty() ? Collections.emptyList() : photoAssetRepository.saveAll(toPersist);

        return new IngestionResult(
                notification.objectKeys().size(),
                saved.size(),
                saved.stream().map(PhotoAsset::getId).toList(),
                skipped,
                failures
        );
    }

    private static String normalizeKey(String key) {
        String trimmed = key.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    private PhotoAsset buildPhotoAsset(
            String bucket,
            String key,
            Event event,
            Photographer photographer,
            HeadObjectResponse head
    ) {
        PhotoAsset asset = new PhotoAsset();
        asset.setBucket(bucket);
        asset.setObjectKey(key);
        asset.setEvent(event);
        asset.setPhotographer(photographer);
        asset.setUploadedAt(Optional.ofNullable(head.lastModified()).orElseGet(Instant::now));
        asset.setCapturedAt(extractCaptureTimestamp(head));
        return asset;
    }

    private HeadObjectResponse fetchObjectMetadata(String bucket, String key) {
        return s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    private Instant extractCaptureTimestamp(HeadObjectResponse head) {
        if (head == null || head.metadata() == null || head.metadata().isEmpty()) {
            return null;
        }
        Map<String, String> metadata = head.metadata();
        for (String captureKey : CAPTURE_METADATA_KEYS) {
            String value = findMetadataValue(metadata, captureKey);
            if (value == null || value.isBlank()) {
                continue;
            }
            Instant instant = parseInstant(value);
            if (instant != null) {
                return instant;
            }
        }
        return null;
    }

    private static String findMetadataValue(Map<String, String> metadata, String targetKey) {
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(targetKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // Fall through
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            // Fall through
        }
        try {
            long epoch = Long.parseLong(value);
            if (epoch > 0) {
                return Instant.ofEpochMilli(epoch);
            }
        } catch (NumberFormatException ignored) {
            // Fall through
        }
        log.debug("Unable to parse capture timestamp metadata '{}'", value);
        return null;
    }

    private String resolveBucket(String overrideBucket) {
        String bucket = overrideBucket != null && !overrideBucket.isBlank()
                ? overrideBucket.trim()
                : defaultBucket;
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 bucket not configured; set aws.s3.bucket or provide it in the event");
        }
        return bucket;
    }

    private void validateNotification(PhotoUploadNotification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Photo upload notification is required");
        }
        if (notification.eventId() == null) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (notification.photographerId() == null) {
            throw new IllegalArgumentException("photographerId is required");
        }
        if (notification.objectKeys() == null || notification.objectKeys().isEmpty()) {
            throw new IllegalArgumentException("objectKeys must not be empty");
        }
    }

    public record PhotoUploadNotification(
            UUID eventId,
            UUID photographerId,
            String bucket,
            List<String> objectKeys
    ) { }

    public record IngestionResult(
            int requestedKeys,
            int storedRecords,
            List<UUID> storedIds,
            List<String> skippedExisting,
            List<String> failedKeys
    ) { }
}
