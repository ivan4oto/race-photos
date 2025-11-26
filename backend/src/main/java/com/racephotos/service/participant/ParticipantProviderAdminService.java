package com.racephotos.service.participant;

import com.racephotos.domain.participant.ParticipantProvider;
import com.racephotos.domain.participant.ParticipantProviderRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class ParticipantProviderAdminService {

    private static final Logger log = LogManager.getLogger(ParticipantProviderAdminService.class);

    private final ParticipantProviderRepository providerRepository;

    public ParticipantProviderAdminService(ParticipantProviderRepository providerRepository) {
        this.providerRepository = Objects.requireNonNull(providerRepository, "providerRepository");
    }

    @Transactional
    public ParticipantProvider create(String displayName, String email, String website) {
        String normalizedName = normalize(displayName);
        if (normalizedName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name is required");
        }

        if (providerRepository.existsByDisplayNameIgnoreCase(normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Provider display name must be unique");
        }

        ParticipantProvider provider = new ParticipantProvider();
        provider.setDisplayName(normalizedName);
        provider.setEmail(normalizeEmail(email));
        provider.setWebsite(normalize(website));

        try {
            ParticipantProvider saved = providerRepository.save(provider);
            log.info("Created participant provider {} with name '{}'", saved.getId(), saved.getDisplayName());
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Provider creation failed for name '{}' due to constraint", normalizedName);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Provider could not be created", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ParticipantProvider> list() {
        return providerRepository.findAll();
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
