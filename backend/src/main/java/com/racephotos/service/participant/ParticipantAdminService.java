package com.racephotos.service.participant;

import com.racephotos.auth.user.User;
import com.racephotos.auth.user.UserRepository;
import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventRepository;
import com.racephotos.domain.participant.Participant;
import com.racephotos.domain.participant.ParticipantProvider;
import com.racephotos.domain.participant.ParticipantProviderRepository;
import com.racephotos.domain.participant.ParticipantRepository;
import com.racephotos.service.participant.dto.ParticipantIngestResult;
import com.racephotos.service.participant.dto.ParticipantRegistration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ParticipantAdminService {

    private static final Logger log = LogManager.getLogger(ParticipantAdminService.class);

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantProviderRepository providerRepository;
    private final UserRepository userRepository;

    public ParticipantAdminService(
            EventRepository eventRepository,
            ParticipantRepository participantRepository,
            ParticipantProviderRepository providerRepository,
            UserRepository userRepository
    ) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        this.participantRepository = Objects.requireNonNull(participantRepository, "participantRepository");
        this.providerRepository = Objects.requireNonNull(providerRepository, "providerRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    }

    @Transactional
    public ParticipantIngestResult ingest(UUID eventId, List<ParticipantRegistration> registrations) {
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event id is required");
        }
        if (registrations == null || registrations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registrations list must not be empty");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        int created = 0;
        int unchanged = 0;
        var outcomes = new java.util.ArrayList<ParticipantIngestResult.RegistrationOutcome>(registrations.size());

        for (ParticipantRegistration registration : registrations) {
            String firstName = normalize(registration.firstName());
            String lastName = normalize(registration.lastName());
            String email = normalizeEmail(registration.email());
            if (firstName == null || lastName == null || email == null) {
                log.debug("Skipping registration with missing required fields");
                outcomes.add(new ParticipantIngestResult.RegistrationOutcome(
                        ParticipantIngestResult.RegistrationStatus.SKIPPED_INVALID,
                        registration.externalRegistrationId(),
                        email,
                        firstName,
                        lastName
                ));
                continue;
            }

            var existing = participantRepository
                    .findByEventIdAndEmailIgnoreCaseAndFirstNameIgnoreCaseAndLastNameIgnoreCase(
                            eventId, email, firstName, lastName
                    );

            if (existing.isPresent()) {
                unchanged++;
                outcomes.add(new ParticipantIngestResult.RegistrationOutcome(
                        ParticipantIngestResult.RegistrationStatus.UNCHANGED,
                        registration.externalRegistrationId(),
                        email,
                        firstName,
                        lastName
                ));
                continue; // duplicate for same event + email + name is ignored
            }

            Participant participant = new Participant();
            participant.setEvent(event);
            participant.setFirstName(firstName);
            participant.setLastName(lastName);
            participant.setEmail(email);
            participant.setExternalRegistrationId(normalize(registration.externalRegistrationId()));
            participant.setRegistrationCreatedAt(cleanDate(registration.registrationCreatedAt()));

            if (registration.providerId() != null) {
                ParticipantProvider provider = providerRepository.findById(registration.providerId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider not found"));
                participant.setProvider(provider);
            }

            // link to User if email matches
            userRepository.findByEmailIgnoreCase(email).ifPresent(participant::setUser);

            participantRepository.save(participant);
            created++;
            outcomes.add(new ParticipantIngestResult.RegistrationOutcome(
                    ParticipantIngestResult.RegistrationStatus.CREATED,
                    registration.externalRegistrationId(),
                    email,
                    firstName,
                    lastName
            ));
        }

        log.info("Participant ingest for event {}: created {} unchanged {}", eventId, created, unchanged);
        return ParticipantIngestResult.of(created, unchanged, outcomes);
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

    private OffsetDateTime cleanDate(OffsetDateTime input) {
        return input;
    }
}
