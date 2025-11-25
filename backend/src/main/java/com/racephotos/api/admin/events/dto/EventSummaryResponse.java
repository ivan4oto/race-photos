package com.racephotos.api.admin.events.dto;

import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventStatus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EventSummaryResponse(
        UUID id,
        String slug,
        String name,
        EventStatus status,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String locationCity,
        String locationCountry,
        Instant updatedAt,
        int photographerCount
) {
    public static EventSummaryResponse from(Event event) {
        int count = event.getPhotographers() == null ? 0 : event.getPhotographers().size();
        return new EventSummaryResponse(
                event.getId(),
                event.getSlug(),
                event.getName(),
                event.getStatus(),
                event.getStartTime(),
                event.getEndTime(),
                event.getLocationCity(),
                event.getLocationCountry(),
                event.getUpdatedAt(),
                count
        );
    }
}
