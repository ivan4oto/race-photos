package com.racephotos.service.event;

import com.racephotos.domain.event.EventOrganizer;
import com.racephotos.domain.event.EventOrganizerRepository;
import com.racephotos.domain.event.EventOrganizerStatus;
import com.racephotos.service.event.dto.CreateEventOrganizerCommand;
import com.racephotos.service.event.dto.UpdateEventOrganizerCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@Service
public class EventOrganizerAdminService {

    private static final Logger log = LogManager.getLogger(EventOrganizerAdminService.class);

    private final EventOrganizerRepository eventOrganizerRepository;

    public EventOrganizerAdminService(EventOrganizerRepository eventOrganizerRepository) {
        this.eventOrganizerRepository = Objects.requireNonNull(eventOrganizerRepository, "eventOrganizerRepository");
    }

    @Transactional
    public EventOrganizer createOrganizer(CreateEventOrganizerCommand command) {
        if (command == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        String slug = normalize(command.slug());
        if (slug == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug must not be blank");
        }
        if (eventOrganizerRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An organizer with this slug already exists");
        }

        String name = normalize(command.name());
        if (name == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must not be blank");
        }

        String email = normalizeEmail(command.email());
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must not be blank");
        }

        EventOrganizer organizer = new EventOrganizer();
        organizer.setSlug(slug);
        organizer.setName(name);
        organizer.setEmail(email);
        organizer.setPhoneNumber(normalize(command.phoneNumber()));
        organizer.setStatus(command.status() == null ? EventOrganizerStatus.ACTIVE : command.status());

        try {
            EventOrganizer saved = eventOrganizerRepository.save(organizer);
            log.info("Created organizer {} with slug '{}'", saved.getId(), saved.getSlug());
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to create organizer with slug '{}' due to constraint violation", slug);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Organizer could not be created", e);
        }
    }

    @Transactional
    public EventOrganizer updateOrganizer(UpdateEventOrganizerCommand command) {
        if (command == null || command.organizerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organizer id is required");
        }

        EventOrganizer organizer = eventOrganizerRepository.findById(command.organizerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organizer not found"));

        String slug = normalize(command.slug());
        if (slug == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug must not be blank");
        }
        eventOrganizerRepository.findBySlug(slug)
                .filter(existing -> !existing.getId().equals(organizer.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "An organizer with this slug already exists");
                });

        String name = normalize(command.name());
        if (name == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must not be blank");
        }

        String email = normalizeEmail(command.email());
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must not be blank");
        }

        organizer.setSlug(slug);
        organizer.setName(name);
        organizer.setEmail(email);
        organizer.setPhoneNumber(normalize(command.phoneNumber()));
        organizer.setStatus(command.status() == null ? EventOrganizerStatus.ACTIVE : command.status());

        try {
            EventOrganizer saved = eventOrganizerRepository.save(organizer);
            log.info("Updated organizer {} with slug '{}'", saved.getId(), saved.getSlug());
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to update organizer with slug '{}' due to constraint violation", slug);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Organizer could not be updated", e);
        }
    }

    @Transactional
    public void disableOrganizer(UUID organizerId) {
        if (organizerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organizer id is required");
        }
        EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organizer not found"));

        organizer.setStatus(EventOrganizerStatus.DISABLED);
        eventOrganizerRepository.save(organizer);
        log.info("Disabled organizer {}", organizer.getId());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(String email) {
        String normalized = normalize(email);
        return normalized == null ? null : normalized.toLowerCase();
    }
}
