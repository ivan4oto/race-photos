package com.racephotos.api.admin.photographers.dto;

import com.racephotos.domain.photographer.Photographer;
import com.racephotos.domain.photographer.PhotographerStatus;

import java.time.Instant;
import java.util.UUID;

public record PhotographerSummaryResponse(
        UUID id,
        String slug,
        String displayName,
        String email,
        PhotographerStatus status,
        String defaultCurrency,
        Instant updatedAt
) {
    public static PhotographerSummaryResponse from(Photographer photographer) {
        return new PhotographerSummaryResponse(
                photographer.getId(),
                photographer.getSlug(),
                photographer.getDisplayName(),
                photographer.getEmail(),
                photographer.getStatus(),
                photographer.getDefaultCurrency(),
                photographer.getUpdatedAt()
        );
    }
}
