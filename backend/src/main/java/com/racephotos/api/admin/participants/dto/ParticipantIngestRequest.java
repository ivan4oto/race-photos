package com.racephotos.api.admin.participants.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ParticipantIngestRequest(
        @NotNull
        @NotEmpty
        List<@Valid ParticipantRegistrationPayload> registrations
) {
}
