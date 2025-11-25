package com.racephotos.api.admin.photographers.dto;

import com.racephotos.api.admin.shared.dto.PricingProfileResponse;
import com.racephotos.domain.photographer.PayoutPreferences;
import com.racephotos.domain.photographer.Photographer;
import com.racephotos.domain.photographer.PhotographerStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PhotographerDetailResponse(
        UUID id,
        String slug,
        String firstName,
        String lastName,
        String displayName,
        String email,
        String phoneNumber,
        String studioName,
        String website,
        String defaultCurrency,
        PhotographerStatus status,
        String biography,
        BigDecimal commissionOverride,
        PricingProfileResponse rateCard,
        PayoutPreferencesResponse payoutPreferences,
        BigDecimal payoutThreshold,
        String internalNotes,
        Instant createdAt,
        Instant updatedAt
) {
    public static PhotographerDetailResponse from(Photographer photographer) {
        PayoutPreferences preferences = photographer.getPayoutPreferences();
        return new PhotographerDetailResponse(
                photographer.getId(),
                photographer.getSlug(),
                photographer.getFirstName(),
                photographer.getLastName(),
                photographer.getDisplayName(),
                photographer.getEmail(),
                photographer.getPhoneNumber(),
                photographer.getStudioName(),
                photographer.getWebsite(),
                photographer.getDefaultCurrency(),
                photographer.getStatus(),
                photographer.getBiography(),
                photographer.getCommissionOverride(),
                PricingProfileResponse.from(photographer.getRateCard()),
                PayoutPreferencesResponse.from(preferences),
                photographer.getPayoutThreshold(),
                photographer.getInternalNotes(),
                photographer.getCreatedAt(),
                photographer.getUpdatedAt()
        );
    }
}
