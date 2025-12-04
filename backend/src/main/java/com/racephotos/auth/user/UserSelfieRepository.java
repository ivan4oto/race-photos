package com.racephotos.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSelfieRepository extends JpaRepository<UserSelfie, UUID> {
    Optional<UserSelfie> findByUserId(UUID userId);
}
