package com.racephotos.domain.photographer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotographerRepository extends JpaRepository<Photographer, UUID> {
    Optional<Photographer> findBySlug(String slug);
    Optional<Photographer> findByEmail(String email);
    List<Photographer> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);
    boolean existsBySlug(String slug);
    boolean existsByEmail(String email);
}
