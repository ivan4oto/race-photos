package com.racephotos.service;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class FaceIndexingService {

    private static final Logger log = LogManager.getLogger(FaceIndexingService.class);
    private static final int MAX_EXTERNAL_IMAGE_ID = 256;

    private final RekognitionClient rekognitionClient;
    private final FaceMetadataRepository metadataRepository;
    private final String bucket;
    private final String collectionId;
    private final AtomicBoolean collectionEnsured = new AtomicBoolean(false);
    private final Object collectionLock = new Object();

    public FaceIndexingService(
            RekognitionClient rekognitionClient,
            FaceMetadataRepository metadataRepository,
            @Value("${aws.s3.bucket:}") String bucket,
            @Value("${aws.rekognition.collection-id:}") String collectionId
    ) {
        this.rekognitionClient = Objects.requireNonNull(rekognitionClient, "rekognitionClient");
        this.metadataRepository = Objects.requireNonNull(metadataRepository, "metadataRepository");
        this.bucket = bucket;
        this.collectionId = collectionId;
    }

    public IndexingReport indexFacesForEvent(String eventId, List<String> objectKeys) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 bucket name (aws.s3.bucket) is not configured");
        }
        if (collectionId == null || collectionId.isBlank()) {
            throw new IllegalStateException("Rekognition collection id (aws.rekognition.collection-id) is not configured");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        if (objectKeys == null || objectKeys.isEmpty()) {
            throw new IllegalArgumentException("objectKeys must contain at least one item");
        }

        ensureCollectionExists();

        Map<String, Integer> facesPerImage = new LinkedHashMap<>();
        List<String> failedKeys = new ArrayList<>();
        int totalFaces = 0;

        for (String key : objectKeys) {
            if (key == null || key.isBlank()) {
                log.warn("Skipping blank S3 key");
                failedKeys.add(key == null ? "<null>" : key);
                continue;
            }
            String normalizedKey = key.trim();
            try {
                IndexFacesResponse response = indexFacesForImage(eventId, normalizedKey);
                List<FaceRecord> faceRecords = response.faceRecords();
                int facesIndexed = faceRecords == null ? 0 : faceRecords.size();
                facesPerImage.put(normalizedKey, facesIndexed);
                totalFaces += facesIndexed;
                if (faceRecords != null) {
                    for (FaceRecord record : faceRecords) {
                        storeMetadata(eventId, normalizedKey, record);
                    }
                }
                log.info("Indexed {} faces for event={} key={} (model v{})", facesIndexed, eventId, normalizedKey, response.faceModelVersion());
            } catch (SdkException e) {
                log.error("Failed to index faces for event={} key={}: {}", eventId, normalizedKey, e.getMessage());
                failedKeys.add(normalizedKey);
            }
        }

        int successfulImages = objectKeys.size() - failedKeys.size();
        return new IndexingReport(
                objectKeys.size(),
                successfulImages,
                totalFaces,
                Map.copyOf(facesPerImage),
                List.copyOf(failedKeys)
        );
    }

    private IndexFacesResponse indexFacesForImage(String eventId, String key) {
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

    private void storeMetadata(String eventId, String key, FaceRecord record) {
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

    private void ensureCollectionExists() {
        if (collectionEnsured.get()) {
            return;
        }
        synchronized (collectionLock) {
            if (collectionEnsured.get()) {
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
            collectionEnsured.set(true);
        }
    }

    private static String buildExternalImageId(String key) {
        return key.replaceAll("[^a-zA-Z0-9_.\\-:]", ":");
    }

    public record IndexingReport(
            int requestedImages,
            int successfullyIndexedImages,
            int totalFaces,
            Map<String, Integer> facesPerImage,
            List<String> failedImages
    ) { }
}
