package com.racephotos.service.event;

import com.racephotos.domain.common.PricingProfile;
import com.racephotos.domain.event.EligibilityMode;
import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventAccessPolicy;
import com.racephotos.domain.event.EventOrganizer;
import com.racephotos.domain.event.EventOrganizerRepository;
import com.racephotos.domain.event.EventOrganizerStatus;
import com.racephotos.domain.event.EventRepository;
import com.racephotos.domain.event.EventStatus;
import com.racephotos.domain.photographer.Photographer;
import com.racephotos.domain.photographer.PhotographerRepository;
import com.racephotos.domain.photo.PhotoAssetRepository;
import com.racephotos.service.event.dto.AccessPolicyData;
import com.racephotos.service.event.dto.CreateEventCommand;
import com.racephotos.service.event.dto.UpdateEventCommand;
import com.racephotos.service.photographer.dto.PhotographerIdentifierData;
import com.racephotos.service.common.dto.PricingProfileData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Service
public class EventAdminService {

    private static final Logger log = LogManager.getLogger(EventAdminService.class);

    private final EventRepository eventRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final PhotographerRepository photographerRepository;
    private final PhotoAssetRepository photoAssetRepository;

    public EventAdminService(
            EventRepository eventRepository,
            EventOrganizerRepository eventOrganizerRepository,
            PhotographerRepository photographerRepository,
            PhotoAssetRepository photoAssetRepository
    ) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        this.eventOrganizerRepository = Objects.requireNonNull(eventOrganizerRepository, "eventOrganizerRepository");
        this.photographerRepository = Objects.requireNonNull(photographerRepository, "photographerRepository");
        this.photoAssetRepository = Objects.requireNonNull(photoAssetRepository, "photoAssetRepository");
    }

    @Transactional
    public Event createEvent(CreateEventCommand command) {
        if (command == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        String slug = normalize(command.slug());
        if (slug == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug must not be blank");
        }
        if (eventRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An event with this slug already exists");
        }

        validateEventTimes(command.startTime(), command.endTime());

        EventOrganizer organizer = resolveOrganizer(command.organizerId());

        Event event = new Event();
        event.setSlug(slug);
        event.setName(normalize(command.name()));
        event.setDescription(normalize(command.description()));
        event.setStatus(command.status() == null ? EventStatus.DRAFT : command.status());
        event.setOrganizerName(organizer != null ? organizer.getName() : normalize(command.organizerName()));
        event.setEventOrganizer(organizer);
        event.setRegistrationProvider(normalize(command.registrationProvider()));
        event.setVectorCollectionId(normalize(command.vectorCollectionId()));
        event.setUploadPrefix(normalize(command.uploadPrefix()));
        event.setTimezone(normalize(command.timezone()));
        event.setStartTime(command.startTime());
        event.setEndTime(command.endTime());
        event.setLocationName(normalize(command.locationName()));
        event.setLocationCity(normalize(command.locationCity()));
        event.setLocationState(normalize(command.locationState()));
        event.setLocationCountry(normalize(command.locationCountry()));
        event.setCoverImageKey(normalize(command.coverImageKey()));
        event.setPlatformCommissionRate(command.platformCommissionRate());
        event.setWatermarkingEnabled(command.watermarkingEnabled());
        event.setAutoPublishMatches(command.autoPublishMatches());
        applyPricing(event, command.defaultPricing());
        applyAccessPolicy(event, command.accessPolicy());
        event.setParticipantMessage(normalize(command.participantMessage()));

        try {
            Event saved = eventRepository.save(event);
            log.info("Created event {} with slug '{}'", saved.getId(), saved.getSlug());
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to create event with slug '{}' due to constraint violation", slug);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event could not be created", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Event> listEvents() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Event getEvent(UUID eventId) {
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event id is required");
        }
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    @Transactional
    public Event updateEvent(UpdateEventCommand command) {
        if (command == null || command.eventId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event id is required");
        }

        Event event = eventRepository.findById(command.eventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        String slug = normalize(command.slug());
        if (slug == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug must not be blank");
        }

        eventRepository.findBySlug(slug)
                .filter(existing -> !existing.getId().equals(event.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "An event with this slug already exists");
                });

        validateEventTimes(command.startTime(), command.endTime());

        EventOrganizer organizer = resolveOrganizer(command.organizerId());

        event.setSlug(slug);
        event.setName(normalize(command.name()));
        event.setDescription(normalize(command.description()));
        event.setStatus(command.status() == null ? EventStatus.DRAFT : command.status());
        event.setOrganizerName(organizer != null ? organizer.getName() : normalize(command.organizerName()));
        event.setEventOrganizer(organizer);
        event.setRegistrationProvider(normalize(command.registrationProvider()));
        event.setVectorCollectionId(normalize(command.vectorCollectionId()));
        event.setUploadPrefix(normalize(command.uploadPrefix()));
        event.setTimezone(normalize(command.timezone()));
        event.setStartTime(command.startTime());
        event.setEndTime(command.endTime());
        event.setLocationName(normalize(command.locationName()));
        event.setLocationCity(normalize(command.locationCity()));
        event.setLocationState(normalize(command.locationState()));
        event.setLocationCountry(normalize(command.locationCountry()));
        event.setCoverImageKey(normalize(command.coverImageKey()));
        event.setPlatformCommissionRate(command.platformCommissionRate());
        event.setWatermarkingEnabled(command.watermarkingEnabled());
        event.setAutoPublishMatches(command.autoPublishMatches());
        applyPricing(event, command.defaultPricing());
        applyAccessPolicy(event, command.accessPolicy());
        event.setParticipantMessage(normalize(command.participantMessage()));

        try {
            Event saved = eventRepository.save(event);
            log.info("Updated event {} with slug '{}'", saved.getId(), saved.getSlug());
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to update event with slug '{}' due to constraint violation", slug);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event could not be updated", e);
        }
    }

    @Transactional
    public Event addPhotographer(UUID eventId, PhotographerIdentifierData identifier) {
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event id is required");
        }
        if (identifier == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photographer identifier is required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        Photographer photographer = resolvePhotographer(identifier);
        event.assignPhotographer(photographer);
        Event saved = eventRepository.save(event);
        log.info("Assigned photographer {} to event {}", photographer.getId(), saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getPhotoPrefixCounts(UUID eventId) {
            log.info("Get photo prefix counts for {}", eventId);
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event id is required");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }

        Instant startTime = Instant.now();
        Map<String, LongAdder> counters = new ConcurrentHashMap<>();
        try (var keys = photoAssetRepository.streamObjectKeysByEventId(eventId)) {
            keys.filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(key -> !key.isEmpty())
                    .forEach(key -> accumulatePrefixes(key, counters));
        }
        Instant endTime = Instant.now();
        log.debug("Time taken to retrieve photo prefix counts: {}", Duration.between(startTime, endTime).toMillis());
        return counters.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().longValue()));
    }

    private void accumulatePrefixes(String objectKey, Map<String, LongAdder> counters) {
        String[] rawSegments = objectKey.split("/");
        List<String> segments = new ArrayList<>(rawSegments.length);
        for (String rawSegment : rawSegments) {
            if (rawSegment == null) {
                continue;
            }
            String segment = rawSegment.trim();
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
        }
        if (segments.size() <= 1) {
            return; // No directory prefixes to count.
        }

        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < segments.size() - 1; i++) {
            prefix.append(segments.get(i)).append('/');
            counters.computeIfAbsent(prefix.toString(), key -> new LongAdder()).increment();
        }
    }

    @Transactional
    public void removePhotographer(UUID eventId, UUID photographerId) {
        if (eventId == null || photographerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event id and photographer id are required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        Photographer photographer = photographerRepository.findById(photographerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photographer not found"));

        event.removePhotographer(photographer);
        eventRepository.save(event);
        log.info("Removed photographer {} from event {}", photographerId, eventId);
    }

    @Transactional(readOnly = true)
    public PhotoAssetSummary getPhotoAssetSummary(UUID eventId) {
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event id is required");
        }
        long indexed = photoAssetRepository.countByEventIdAndIndexStatusIsNotNull(eventId);
        long unindexed = photoAssetRepository.countByEventIdAndIndexStatusIsNull(eventId);
        return new PhotoAssetSummary(indexed, unindexed);
    }

    private void applyPricing(Event event, PricingProfileData data) {
        PricingProfile profile = event.getDefaultPricing();
        if (profile == null) {
            profile = new PricingProfile();
            event.setDefaultPricing(profile);
        }
        if (data == null) {
            return;
        }
        profile.setPricePerPhoto(data.pricePerPhoto());
        profile.setBundlePrice(data.bundlePrice());
        profile.setBundleSize(data.bundleSize());
        profile.setCurrencyCode(normalizeUpper(data.currencyCode()));
    }

    private void applyAccessPolicy(Event event, AccessPolicyData data) {
        EventAccessPolicy policy = event.getAccessPolicy();
        if (policy == null) {
            policy = new EventAccessPolicy();
            event.setAccessPolicy(policy);
        }
        if (data == null) {
            return;
        }
        policy.setMode(data.mode() == null ? EligibilityMode.NONE : data.mode());
        policy.setProvider(normalize(data.provider()));
        policy.setConfiguration(normalize(data.configuration()));
    }

    private void validateEventTimes(OffsetDateTime start, OffsetDateTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
    }

    private Photographer resolvePhotographer(PhotographerIdentifierData identifier) {
        String slug = normalize(identifier.slug());
        String email = normalizeEmail(identifier.email());
        String firstName = normalize(identifier.firstName());
        String lastName = normalize(identifier.lastName());

        if (slug != null) {
            return photographerRepository.findBySlug(slug)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photographer slug not found"));
        }
        if (email != null) {
            return photographerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photographer email not found"));
        }
        if (firstName != null && lastName != null) {
            var matches = photographerRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName, lastName);
            if (matches.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photographer name not found");
            }
            if (matches.size() > 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple photographers match this name");
            }
            return matches.get(0);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No photographer identifier provided");
    }

    private EventOrganizer resolveOrganizer(UUID organizerId) {
        if (organizerId == null) {
            return null;
        }
        EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organizer not found"));
        if (organizer.getStatus() == EventOrganizerStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organizer is disabled");
        }
        return organizer;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String normalizeEmail(String email) {
        String normalized = normalize(email);
        return normalized == null ? null : normalized.toLowerCase();
    }

    public record PhotoAssetSummary(long indexedPhotoCount, long unindexedPhotoCount) { }
}
