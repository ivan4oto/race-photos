package com.racephotos.api.admin.events.dto;

import com.racephotos.domain.event.EventOrganizer;

import java.util.UUID;

public record EventOrganizerRefResponse(
        UUID id,
        String slug
) {
    public static EventOrganizerRefResponse from(EventOrganizer organizer) {
        if (organizer == null) {
            return null;
        }
        return new EventOrganizerRefResponse(organizer.getId(), organizer.getSlug());
    }
}
