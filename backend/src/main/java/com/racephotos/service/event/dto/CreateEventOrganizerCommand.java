package com.racephotos.service.event.dto;

import com.racephotos.domain.event.EventOrganizerStatus;

public record CreateEventOrganizerCommand(
        String slug,
        String name,
        String email,
        String phoneNumber,
        EventOrganizerStatus status
) { }
