package com.racephotos.service.photographer.dto;

public record PhotographerIdentifierData(
        String slug,
        String email,
        String firstName,
        String lastName
) {
}
