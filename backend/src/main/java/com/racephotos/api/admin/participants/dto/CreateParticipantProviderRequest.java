package com.racephotos.api.admin.participants.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateParticipantProviderRequest(
        @NotBlank
        @Size(max = 160)
        String displayName,
        @Email
        @Size(max = 160)
        String email,
        @Size(max = 255)
        String website
) {
}
