package com.racephotos.domain.participant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ParticipantProviderRepository extends JpaRepository<ParticipantProvider, UUID> {
}
