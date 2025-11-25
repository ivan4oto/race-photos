package com.racephotos.api.search.dto;

import jakarta.validation.constraints.NotBlank;

public record FaceSearchRequest(
        @NotBlank
        String eventId,
        @NotBlank
        String photoKey
) {
}
