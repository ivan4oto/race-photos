package com.racephotos.service;

import com.racephotos.auth.user.User;
import com.racephotos.auth.user.UserRepository;
import com.racephotos.domain.common.PricingProfile;
import com.racephotos.domain.photographer.PayoutMethod;
import com.racephotos.domain.photographer.PayoutPreferences;
import com.racephotos.domain.photographer.Photographer;
import com.racephotos.domain.photographer.PhotographerRepository;
import com.racephotos.domain.photographer.PhotographerStatus;
import com.racephotos.service.dto.CreatePhotographerCommand;
import com.racephotos.service.dto.PayoutPreferencesData;
import com.racephotos.service.dto.PricingProfileData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
public class PhotographerAdminService {

    private static final Logger log = LogManager.getLogger(PhotographerAdminService.class);

    private final PhotographerRepository photographerRepository;
    private final UserRepository userRepository;

    public PhotographerAdminService(
            PhotographerRepository photographerRepository,
            UserRepository userRepository
    ) {
        this.photographerRepository = Objects.requireNonNull(photographerRepository, "photographerRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    }

    @Transactional
    public Photographer createPhotographer(CreatePhotographerCommand command) {
        if (command == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        UUID createdById = command.createdByUserId();
        if (createdById == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session is required");
        }

        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session is invalid"));

        String slug = normalize(command.slug());
        if (slug == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug must not be blank");
        }
        if (photographerRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A photographer with this slug already exists");
        }

        String email = normalizeEmail(command.email());
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must not be blank");
        }
        if (photographerRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A photographer with this email already exists");
        }

        BigDecimal commissionOverride = normalizeDecimal(command.commissionOverride());
        if (commissionOverride != null &&
                (commissionOverride.compareTo(BigDecimal.ZERO) < 0 || commissionOverride.compareTo(BigDecimal.ONE) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commission override must be between 0 and 1");
        }

        BigDecimal payoutThreshold = normalizeDecimal(command.payoutThreshold());
        if (payoutThreshold != null && payoutThreshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payout threshold must be non-negative");
        }

        Photographer photographer = new Photographer();
        photographer.setSlug(slug);
        photographer.setFirstName(normalize(command.firstName()));
        photographer.setLastName(normalize(command.lastName()));
        photographer.setDisplayName(normalize(command.displayName()));
        photographer.setEmail(email);
        photographer.setPhoneNumber(normalize(command.phoneNumber()));
        photographer.setStudioName(normalize(command.studioName()));
        photographer.setWebsite(normalize(command.website()));
        photographer.setDefaultCurrency(normalizeUpper(command.defaultCurrency()));
        photographer.setStatus(command.status() == null ? PhotographerStatus.ONBOARDING : command.status());
        photographer.setBiography(normalize(command.biography()));
        photographer.setCommissionOverride(commissionOverride);
        photographer.setPayoutThreshold(payoutThreshold);
        photographer.setInternalNotes(normalize(command.internalNotes()));
        photographer.setCreatedBy(creator);

        applyRateCard(photographer, command.rateCard());
        applyPayoutPreferences(photographer, command.payoutPreferences());

        try {
            Photographer saved = photographerRepository.save(photographer);
            log.info("User {} created photographer {} with slug '{}'", creator.getEmail(), saved.getId(), saved.getSlug());
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to create photographer with slug '{}' due to constraint violation", slug);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Photographer could not be created", e);
        }
    }

    private void applyRateCard(Photographer photographer, PricingProfileData data) {
        if (data == null) {
            return;
        }
        PricingProfile profile = photographer.getRateCard();
        if (profile == null) {
            profile = new PricingProfile();
            photographer.setRateCard(profile);
        }
        profile.setPricePerPhoto(data.pricePerPhoto());
        profile.setBundlePrice(data.bundlePrice());
        profile.setBundleSize(data.bundleSize());
        profile.setCurrencyCode(normalizeUpper(data.currencyCode()));
    }

    private void applyPayoutPreferences(Photographer photographer, PayoutPreferencesData data) {
        if (data == null) {
            return;
        }
        PayoutPreferences preferences = photographer.getPayoutPreferences();
        if (preferences == null) {
            preferences = new PayoutPreferences();
            photographer.setPayoutPreferences(preferences);
        }
        preferences.setMethod(data.method() == null ? PayoutMethod.UNSPECIFIED : data.method());
        preferences.setAccountReference(normalize(data.accountReference()));
        preferences.setPayoutEmail(normalizeEmail(data.payoutEmail()));
        preferences.setBankAccountLast4(normalize(data.bankAccountLast4()));
        preferences.setBankRoutingNumber(normalize(data.bankRoutingNumber()));
        preferences.setTaxId(normalize(data.taxId()));
        preferences.setMetadata(normalize(data.metadata()));
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

    private String normalizeEmail(String email) {
        String normalized = normalize(email);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private BigDecimal normalizeDecimal(BigDecimal value) {
        return value;
    }
}
