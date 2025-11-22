package com.racephotos.service.dto;

import com.racephotos.domain.event.EventStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateEventCommand(
        String slug,
        String name,
        String description,
        EventStatus status,
        String organizerName,
        String registrationProvider,
        String vectorCollectionId,
        String uploadPrefix,
        String timezone,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String locationName,
        String locationCity,
        String locationState,
        String locationCountry,
        String coverImageKey,
        BigDecimal platformCommissionRate,
        boolean watermarkingEnabled,
        boolean autoPublishMatches,
        PricingProfileData defaultPricing,
        AccessPolicyData accessPolicy,
        String participantMessage
) {}
