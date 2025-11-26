package com.racephotos.api.admin.participants.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ParticipantRegistrationPayload(
        @NotBlank @Size(max = 120) String firstName,
        @NotBlank @Size(max = 120) String lastName,
        @NotBlank @Email @Size(max = 160) String email,
        UUID providerId,
        @Size(max = 160) String externalRegistrationId
) {
}
