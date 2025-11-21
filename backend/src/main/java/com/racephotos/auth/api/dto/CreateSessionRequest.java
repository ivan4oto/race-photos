package com.racephotos.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionRequest(
    @NotBlank String idToken,
    @NotBlank String accessToken
) {
}
