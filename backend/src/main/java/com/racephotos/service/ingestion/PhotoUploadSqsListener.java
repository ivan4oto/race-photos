package com.racephotos.service.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventRepository;
import com.racephotos.domain.photographer.Photographer;
import com.racephotos.domain.photographer.PhotographerRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class PhotoUploadSqsListener {

    private static final Logger log = LogManager.getLogger(PhotoUploadSqsListener.class);

    private final ObjectMapper objectMapper;
    private final PhotoAssetIngestionService ingestionService;
    private final EventRepository eventRepository;
    private final PhotographerRepository photographerRepository;

    public PhotoUploadSqsListener(
            ObjectMapper objectMapper,
            PhotoAssetIngestionService ingestionService,
            EventRepository eventRepository,
            PhotographerRepository photographerRepository
    ) {
        this.objectMapper = objectMapper;
        this.ingestionService = ingestionService;
        this.eventRepository = eventRepository;
        this.photographerRepository = photographerRepository;
    }

    @SqsListener("${aws.sqs.photo-upload-queue:photo-upload-event-queue-dev}")
    public void onMessage(String messageBody) {
        S3EventNotification notification = parse(messageBody);
        Map<IngestionKey, List<String>> groupedKeys = new HashMap<>();
        Map<IngestionKey, String> buckets = new HashMap<>();

        for (S3Record record : notification.records()) {
            String bucket = record.s3().bucket().name();
            String key = decodeKey(record.s3().object().key());

            Event event = resolveEvent(key)
                    .orElse(null);
            if (event == null) {
                log.warn("Skipping key={} because no event uploadPrefix matched", key);
                continue;
            }
            Photographer photographer = resolvePhotographer(event, key)
                    .orElse(null);
            if (photographer == null) {
                log.warn("Skipping key={} because no photographer could be inferred", key);
                continue;
            }

            IngestionKey groupingKey = new IngestionKey(event.getId(), photographer.getId());
            groupedKeys.computeIfAbsent(groupingKey, k -> new ArrayList<>()).add(key);
            buckets.putIfAbsent(groupingKey, bucket);
        }

        for (Map.Entry<IngestionKey, List<String>> entry : groupedKeys.entrySet()) {
            IngestionKey groupingKey = entry.getKey();
            List<String> keys = entry.getValue();
            String bucket = buckets.get(groupingKey);

            PhotoAssetIngestionService.PhotoUploadNotification uploadNotification =
                    new PhotoAssetIngestionService.PhotoUploadNotification(
                            groupingKey.eventId(),
                            groupingKey.photographerId(),
                            bucket,
                            List.copyOf(keys)
                    );
            var result = ingestionService.ingestPhotoUpload(uploadNotification);
            log.info("Ingested {} photo assets for event={} photographer={} (requested {}, skipped {}, failed {})",
                    result.storedRecords(),
                    groupingKey.eventId(),
                    groupingKey.photographerId(),
                    result.requestedKeys(),
                    result.skippedExisting().size(),
                    result.failedKeys().size());

            if (!result.failedKeys().isEmpty()) {
                throw new IllegalStateException("Failed to ingest keys: " + result.failedKeys());
            }
        }
    }

    private Optional<Event> resolveEvent(String objectKey) {
        List<Event> events = eventRepository.findAll();
        for (Event event : events) {
            String prefix = normalizePrefix(event.getUploadPrefix());
            if (prefix != null && objectKey.startsWith(prefix)) {
                return Optional.of(event);
            }
        }
        return Optional.empty();
    }

    private Optional<Photographer> resolvePhotographer(Event event, String objectKey) {
        String prefix = normalizePrefix(event.getUploadPrefix());
        if (prefix == null || objectKey.length() <= prefix.length()) {
            return Optional.empty();
        }
        String remainder = objectKey.substring(prefix.length());
        if (remainder.startsWith("/")) {
            remainder = remainder.substring(1);
        }
        String[] parts = remainder.split("/", 2);
        if (parts.length == 0 || parts[0].isBlank()) {
            return Optional.empty();
        }
        String slug = parts[0];
        Optional<Photographer> photographer = photographerRepository.findBySlug(slug);
        if (photographer.isPresent()) {
            return photographer;
        }
        // Fallback: if slug is not found but event has a single photographer assigned, use it.
        Set<Photographer> photographers = event.getPhotographers();
        if (photographers != null && photographers.size() == 1) {
            log.warn("Photographer slug={} not found for event={} but event has a single photographer assigned; using it", slug, event.getId());
            return Optional.of(photographers.iterator().next());
        }
        return Optional.empty();
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null) {
            return null;
        }
        String normalized = prefix.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String decodeKey(String key) {
        return URLDecoder.decode(key.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private S3EventNotification parse(String messageBody) {
        try {
            return objectMapper.readValue(messageBody, S3EventNotification.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to parse S3 event notification", e);
        }
    }

    private record IngestionKey(UUID eventId, UUID photographerId) { }

    private record S3EventNotification(List<S3Record> Records) {
        public List<S3Record> records() {
            return Records == null ? List.of() : Records;
        }
    }

    private record S3Record(S3Entity s3) { }

    private record S3Entity(S3Bucket bucket, S3Object object) { }

    private record S3Bucket(String name) { }

    private record S3Object(String key) { }
}
