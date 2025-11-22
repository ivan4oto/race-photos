package com.racephotos.service;

import com.racephotos.domain.common.PricingProfile;
import com.racephotos.domain.event.EligibilityMode;
import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventAccessPolicy;
import com.racephotos.domain.event.EventRepository;
import com.racephotos.domain.event.EventStatus;
import com.racephotos.service.dto.AccessPolicyData;
import com.racephotos.service.dto.CreateEventCommand;
import com.racephotos.service.dto.PricingProfileData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Service
public class EventAdminService {

    private static final Logger log = LogManager.getLogger(EventAdminService.class);

    private final EventRepository eventRepository;

    public EventAdminService(EventRepository eventRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
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

        Event event = new Event();
        event.setSlug(slug);
        event.setName(normalize(command.name()));
        event.setDescription(normalize(command.description()));
        event.setStatus(command.status() == null ? EventStatus.DRAFT : command.status());
        event.setOrganizerName(normalize(command.organizerName()));
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
}
