package com.racephotos.api.admin.events.dto;

import com.racephotos.api.admin.photographers.dto.PhotographerSummaryResponse;
import com.racephotos.api.admin.shared.dto.PricingProfileResponse;
import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EventDetailResponse(
        UUID id,
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
        PricingProfileResponse defaultPricing,
        AccessPolicyResponse accessPolicy,
        String participantMessage,
        Instant createdAt,
        Instant updatedAt,
        List<PhotographerSummaryResponse> photographers
) {
    public static EventDetailResponse from(Event event) {
        return new EventDetailResponse(
                event.getId(),
                event.getSlug(),
                event.getName(),
                event.getDescription(),
                event.getStatus(),
                event.getOrganizerName(),
                event.getRegistrationProvider(),
                event.getVectorCollectionId(),
                event.getUploadPrefix(),
                event.getTimezone(),
                event.getStartTime(),
                event.getEndTime(),
                event.getLocationName(),
                event.getLocationCity(),
                event.getLocationState(),
                event.getLocationCountry(),
                event.getCoverImageKey(),
                event.getPlatformCommissionRate(),
                event.isWatermarkingEnabled(),
                event.isAutoPublishMatches(),
                PricingProfileResponse.from(event.getDefaultPricing()),
                AccessPolicyResponse.from(event.getAccessPolicy()),
                event.getParticipantMessage(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                event.getPhotographers()
                        .stream()
                        .map(PhotographerSummaryResponse::from)
                        .toList()
        );
    }
}
