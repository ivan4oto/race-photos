package com.racephotos.api.event;

import com.racephotos.domain.event.Event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccessibleEventResponse(
        UUID id,
        String name,
        String slug,
        OffsetDateTime startDate,
        String coverImageUrl
) {
    public static AccessibleEventResponse from(Event event, String coverImageUrl) {
        return new AccessibleEventResponse(
                event.getId(),
                event.getName(),
                event.getSlug(),
                event.getStartTime(),
                coverImageUrl
        );
    }
}
