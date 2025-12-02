package com.racephotos.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventAccessGrantRepository extends JpaRepository<EventAccessGrant, UUID> {

    Optional<EventAccessGrant> findByEmailIgnoreCaseAndEventId(String email, UUID eventId);

    List<EventAccessGrant> findByUserIdAndStatus(UUID userId, AccessGrantStatus status);

    List<EventAccessGrant> findByEmailIgnoreCaseAndStatus(String email, AccessGrantStatus status);
}
