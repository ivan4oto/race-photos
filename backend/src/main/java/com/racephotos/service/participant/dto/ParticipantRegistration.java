package com.racephotos.service.participant.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ParticipantRegistration(
        String firstName,
        String lastName,
        String email,
        UUID providerId,
        String externalRegistrationId
) {
}
