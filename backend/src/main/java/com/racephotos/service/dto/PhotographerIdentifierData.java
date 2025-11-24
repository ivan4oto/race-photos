package com.racephotos.service.dto;

public record PhotographerIdentifierData(
        String slug,
        String email,
        String firstName,
        String lastName
) {
}
