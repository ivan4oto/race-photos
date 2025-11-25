package com.racephotos.api.admin.events.dto;

import com.racephotos.service.photographer.dto.PhotographerIdentifierData;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddPhotographerToEventRequest(
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
        @Size(max = 80)
        String slug,
        @Email
        @Size(max = 160)
        String email,
        @Size(max = 80)
        String firstName,
        @Size(max = 80)
        String lastName
) {
    @AssertTrue(message = "Provide slug, email, or first+last name")
    public boolean hasIdentifier() {
        boolean hasSlug = slug != null && !slug.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasFullName = firstName != null && !firstName.isBlank()
                && lastName != null && !lastName.isBlank();
        return hasSlug || hasEmail || hasFullName;
    }

    public PhotographerIdentifierData toCommand() {
        return new PhotographerIdentifierData(
                normalize(slug),
                normalize(email),
                normalize(firstName),
                normalize(lastName)
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
