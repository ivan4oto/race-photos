package com.racephotos.service.dto;

import com.racephotos.domain.photographer.PhotographerStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePhotographerCommand(
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
        PricingProfileData rateCard,
        PayoutPreferencesData payoutPreferences,
        BigDecimal payoutThreshold,
        String internalNotes,
        UUID createdByUserId
) {
}
