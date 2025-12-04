package com.racephotos.domain.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventOrganizerRepository extends JpaRepository<EventOrganizer, UUID> {
    Optional<EventOrganizer> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
