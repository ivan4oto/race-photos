package com.racephotos.api.admin.events.dto;

import com.racephotos.domain.event.EventOrganizer;
import com.racephotos.domain.event.EventOrganizerStatus;

import java.time.Instant;
import java.util.UUID;

public record EventOrganizerResponse(
        UUID id,
        String slug,
        String name,
        String email,
        String phoneNumber,
        EventOrganizerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventOrganizerResponse from(EventOrganizer organizer) {
        return new EventOrganizerResponse(
                organizer.getId(),
                organizer.getSlug(),
                organizer.getName(),
                organizer.getEmail(),
                organizer.getPhoneNumber(),
                organizer.getStatus(),
                organizer.getCreatedAt(),
                organizer.getUpdatedAt()
        );
    }
}
