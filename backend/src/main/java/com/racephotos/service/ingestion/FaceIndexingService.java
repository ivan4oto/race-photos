package com.racephotos.service.ingestion;

import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventRepository;
import com.racephotos.domain.photo.PhotoAsset;
import com.racephotos.domain.photo.PhotoAssetRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CreateCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.FaceRecord;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.IndexFacesRequest;
import software.amazon.awssdk.services.rekognition.model.IndexFacesResponse;
import software.amazon.awssdk.services.rekognition.model.QualityFilter;
import software.amazon.awssdk.services.rekognition.model.ResourceNotFoundException;
import software.amazon.awssdk.services.rekognition.model.S3Object;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;

@Service
@SuppressWarnings("preview")
public class FaceIndexingService {

    private static final Logger log = LogManager.getLogger(FaceIndexingService.class);
    private static final int MAX_EXTERNAL_IMAGE_ID = 256;

    private final RekognitionClient rekognitionClient;
    private final FaceMetadataRepository metadataRepository;
    private final PhotoAssetRepository photoAssetRepository;
    private final EventRepository eventRepository;
    private final String bucket;
    private final int maxConcurrentIndexing;
    private final Set<String> ensuredCollections = Collections.synchronizedSet(new HashSet<>());
    private final Object collectionLock = new Object();

    public FaceIndexingService(
            RekognitionClient rekognitionClient,
            FaceMetadataRepository metadataRepository,
            PhotoAssetRepository photoAssetRepository,
            EventRepository eventRepository,
            @Value("${aws.s3.bucket:}") String bucket,
            @Value("${aws.rekognition.index-faces.max-concurrency:5}") int maxConcurrentIndexing
    ) {
        this.rekognitionClient = Objects.requireNonNull(rekognitionClient, "rekognitionClient");
        this.metadataRepository = Objects.requireNonNull(metadataRepository, "metadataRepository");
        this.photoAssetRepository = Objects.requireNonNull(photoAssetRepository, "photoAssetRepository");
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        this.bucket = bucket;
        this.maxConcurrentIndexing = Math.max(1, maxConcurrentIndexing);
    }

    public IndexingReport indexUnindexedPhotoAssets(UUID eventId) {
        log.info("Starting indexing of unindexed photos for event {}", eventId);
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new IllegalArgumentException("Invalid eventId: " + eventId));
        String collectionId = event.getVectorCollectionId();
        ensureCollectionExists(collectionId);
        List<PhotoAsset> assets = photoAssetRepository.findByEventIdAndIndexStatusIsNull(eventId);

        int requested = assets.size();
        int successful = 0;
        int totalFaces = 0;
        Map<String, Integer> facesPerImage = new LinkedHashMap<>();
        List<String> failedKeys = new ArrayList<>();

        Semaphore permits = new Semaphore(maxConcurrentIndexing);
        List<StructuredTaskScope.Subtask<AssetIndexResult>> subtasks = new ArrayList<>();

        try (var scope = new StructuredTaskScope<AssetIndexResult>()) {
            for (PhotoAsset asset : assets) {
                UUID assetId = asset.getId();
                String key = asset.getObjectKey();
                subtasks.add(scope.fork(() -> {
                    permits.acquire();
                    try {
                        return indexSingleAsset(collectionId, eventId, assetId, key);
                    } finally {
                        permits.release();
                    }
                }));
            }
            scope.join();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Indexing interrupted", ie);
        }

        for (StructuredTaskScope.Subtask<AssetIndexResult> subtask : subtasks) {
            if (subtask.state() != StructuredTaskScope.Subtask.State.SUCCESS) {
                continue;
            }
            AssetIndexResult result = subtask.get();
            if (result.success()) {
                successful++;
                totalFaces += result.facesIndexed();
                facesPerImage.put(result.key(), result.facesIndexed());
            } else {
                failedKeys.add(result.key());
            }
        }
        log.info("Finished indexing of unindexed photos for event {}: {}/{} images indexed ({} failed)",
                eventId, successful, requested, failedKeys.size());
        return new IndexingReport(
                requested,
                successful,
                totalFaces,
                Map.copyOf(facesPerImage),
                List.copyOf(failedKeys)
        );
    }

    private IndexFacesResponse indexFacesForImage(String collectionId, String key) {
        IndexFacesRequest request = IndexFacesRequest.builder()
                .collectionId(collectionId)
                .externalImageId(buildExternalImageId(key))
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

    private void storeMetadata(String collectionId, String eventId, String key, FaceRecord record) {
        if (record == null || record.face() == null) {
            return;
        }
        metadataRepository.saveFaceRecord(
                collectionId,
                record.face().faceId(),
                eventId,
                bucket,
                key,
                record.face().imageId(),
                record.face().boundingBox(),
                record.face().confidence()
        );
    }

    private void ensureCollectionExists(String collectionId) {
        if (ensuredCollections.contains(collectionId)) {
            return;
        }
        synchronized (collectionLock) {
            if (ensuredCollections.contains(collectionId)) {
                return;
            }
            try {
                rekognitionClient.describeCollection(
                        DescribeCollectionRequest.builder()
                                .collectionId(collectionId)
                                .build()
                );
                log.debug("Rekognition collection '{}' already exists", collectionId);
            } catch (ResourceNotFoundException notFound) {
                log.info("Rekognition collection '{}' not found, creating it", collectionId);
                rekognitionClient.createCollection(CreateCollectionRequest.builder()
                        .collectionId(collectionId)
                        .build());
            }
            ensuredCollections.add(collectionId);
        }
    }

    private static String buildExternalImageId(String key) {
        return key.replaceAll("[^a-zA-Z0-9_.\\-:]", ":");
    }

    private AssetIndexResult indexSingleAsset(String collectionId, UUID eventId, UUID assetId, String rawKey) {
        String key = normalizeKey(rawKey);
        if (key == null || key.isBlank()) {
            log.warn("Skipping blank S3 key for asset={}", assetId);
            markAssetFailed(assetId);
            return new AssetIndexResult(rawKey == null ? "<null>" : rawKey, 0, false);
        }

        try {
            IndexFacesResponse response = indexFacesForImage(collectionId, key);
            List<FaceRecord> faceRecords = response.faceRecords();
            int facesIndexed = faceRecords == null ? 0 : faceRecords.size();
            if (faceRecords != null) {
                for (FaceRecord record : faceRecords) {
                    storeMetadata(collectionId, eventId.toString(), key, record);
                }
            }
            markAssetSuccess(assetId);
            log.debug("Indexed {} faces for event={} key={} (model v{})", facesIndexed, eventId, key, response.faceModelVersion());
            return new AssetIndexResult(key, facesIndexed, true);
        } catch (SdkException e) {
            log.error("Failed to index faces for event={} key={}: {}", eventId, key, e.getMessage());
            markAssetFailed(assetId);
            return new AssetIndexResult(key, 0, false);
        } catch (Exception e) {
            log.error("Unexpected error indexing faces for event={} key={}: {}", eventId, key, e.getMessage(), e);
            markAssetFailed(assetId);
            return new AssetIndexResult(key, 0, false);
        }
    }

    private void markAssetSuccess(UUID assetId) {
        photoAssetRepository.findById(assetId).ifPresent(asset -> {
            asset.setIndexStatus("SUCCESS");
            asset.setIndexedAt(Instant.now());
            photoAssetRepository.save(asset);
        });
    }

    private void markAssetFailed(UUID assetId) {
        photoAssetRepository.findById(assetId).ifPresent(asset -> {
            asset.setIndexStatus("FAILED");
            photoAssetRepository.save(asset);
        });
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    public record IndexingReport(
            int requestedImages,
            int successfullyIndexedImages,
            int totalFaces,
            Map<String, Integer> facesPerImage,
            List<String> failedImages
    ) { }

    private record AssetIndexResult(String key, int facesIndexed, boolean success) { }
}
