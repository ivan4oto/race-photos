package com.racephotos.service.search;

import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.BoundingBox;
import software.amazon.awssdk.services.rekognition.model.Face;
import software.amazon.awssdk.services.rekognition.model.FaceMatch;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageRequest;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageResponse;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.util.*;
import java.time.Duration;

import com.racephotos.service.ingestion.FaceMetadataRepository;

@Service
public class FaceSearchService {

    private static final Logger log = LogManager.getLogger(FaceSearchService.class);

    private final RekognitionClient rekognitionClient;
    private final FaceMetadataRepository metadataRepository;
    private final EventRepository eventRepository;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final Integer maxFaces;
    private final Float similarityThreshold;
    private final Duration presignExpiration;

    public FaceSearchService(
            RekognitionClient rekognitionClient,
            FaceMetadataRepository metadataRepository,
            EventRepository eventRepository,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket:}") String bucket,
            @Value("${aws.rekognition.search.max-faces}") Integer maxFaces,
            @Value("${aws.rekognition.search.threshold}") Float similarityThreshold,
            @Value("${aws.s3.presign.expiration-seconds:3600}") Long presignExpirationSeconds
    ) {
        this.rekognitionClient = Objects.requireNonNull(rekognitionClient, "rekognitionClient");
        this.metadataRepository = Objects.requireNonNull(metadataRepository, "metadataRepository");
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        this.s3Presigner = Objects.requireNonNull(s3Presigner, "s3Presigner");
        this.bucket = bucket;
        this.maxFaces = maxFaces;
        this.similarityThreshold = similarityThreshold;
        this.presignExpiration = Duration.ofSeconds(
                Optional.ofNullable(presignExpirationSeconds).filter(v -> v > 0).orElse(3600L)
        );
    }

    public FaceSearchResult searchFaces(String eventId, String photoKey) {
        long startNanos = System.nanoTime();
        log.debug("Starting face search for event {} and probe key {}", eventId, photoKey);
        String normalizedKey = validateInputs(eventId, photoKey);
        Event event = eventRepository.findById(UUID.fromString(eventId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        String collectionId = event.getVectorCollectionId();
        SearchFacesByImageResponse response = runSearch(normalizedKey, collectionId);

        Map<String, AggregatedMatch> matchesByPhoto = new LinkedHashMap<>();
        List<FaceMatch> faceMatches = response.faceMatches();
        if (faceMatches != null) {
            for (FaceMatch faceMatch : faceMatches) {
                processMatch(eventId, normalizedKey, faceMatch, matchesByPhoto);
            }
        }

        List<AggregatedMatch> aggregatedMatches = List.copyOf(matchesByPhoto.values());
        List<Match> matchesWithUrls = aggregatedMatches.stream()
                .map(this::toPresignedMatch)
                .toList();

        FaceSearchResult result = new FaceSearchResult(eventId, normalizedKey, matchesWithUrls);
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        log.debug(
                "Finished face search for event {} key {} -> {} aggregated matches ({} raw Rekognition matches) in {} ms",
                eventId,
                normalizedKey,
                aggregatedMatches.size(),
                faceMatches == null ? 0 : faceMatches.size(),
                elapsedMillis
        );
        return result;
    }

    private void processMatch(String eventId, String probeKey, FaceMatch faceMatch, Map<String, AggregatedMatch> matchesByPhoto) {
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
                AggregatedMatch candidate = new AggregatedMatch(
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

    private SearchFacesByImageResponse runSearch(String photoKey, String collectionId) {
        try {
            SearchFacesByImageRequest.Builder searchFacesRequest = SearchFacesByImageRequest.builder()
                    .collectionId(collectionId)
                    .image(Image.builder()
                            .s3Object(S3Object.builder()
                                    .bucket(bucket)
                                    .name(photoKey)
                                    .build())
                            .build());

            if (maxFaces != null) {
                searchFacesRequest.maxFaces(maxFaces);
            }
            if (similarityThreshold != null) {
                searchFacesRequest.faceMatchThreshold(similarityThreshold);
            }

            log.debug(
                    "Invoking Rekognition searchFacesByImage for bucket {} key {} collection {} maxFaces={} threshold={}",
                    bucket,
                    photoKey,
                    collectionId,
                    maxFaces,
                    similarityThreshold
            );
            return rekognitionClient.searchFacesByImage(searchFacesRequest.build());
        } catch (SdkException e) {
            log.error("searchFacesByImage failed for key {}", photoKey, e);
            throw e;
        }
    }

    private String validateInputs(String eventId, String photoKey) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 bucket name (aws.s3.bucket) is not configured");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        if (photoKey == null || photoKey.isBlank()) {
            throw new IllegalArgumentException("photoKey must not be blank");
        }
        return photoKey.trim();
    }

    private Match toPresignedMatch(AggregatedMatch match) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(match.photoKey())
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(presignExpiration)
                .getObjectRequest(getRequest)
                .build();

        String presignedUrl = s3Presigner.presignGetObject(presignRequest)
                .url()
                .toExternalForm();

        return new Match(presignedUrl, match.faceId(), match.similarity(), match.confidence(), match.boundingBox());
    }

    public record FaceSearchResult(String eventId, String probePhotoKey, List<Match> matches) {}

    private record AggregatedMatch(String photoKey, String faceId, float similarity, Float confidence, BoundingBox boundingBox) {}

    public record Match(String photoUrl, String faceId, float similarity, Float confidence, BoundingBox boundingBox) {}
}
