package com.racephotos.service.event.dto;

import com.racephotos.domain.event.EventOrganizerStatus;

import java.util.UUID;

public record UpdateEventOrganizerCommand(
        UUID organizerId,
        String slug,
        String name,
        String email,
        String phoneNumber,
        EventOrganizerStatus status
) { }
