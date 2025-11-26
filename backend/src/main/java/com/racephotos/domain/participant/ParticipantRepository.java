package com.racephotos.domain.participant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    Optional<Participant> findByEventIdAndEmailIgnoreCaseAndFirstNameIgnoreCaseAndLastNameIgnoreCase(
            UUID eventId,
            String email,
            String firstName,
            String lastName
    );
}
