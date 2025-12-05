package com.racephotos.api.admin.events.dto;

import com.racephotos.domain.event.EventOrganizerStatus;
import com.racephotos.service.event.dto.UpdateEventOrganizerCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateEventOrganizerRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
        @Size(max = 80)
        String slug,
        @NotBlank
        @Size(max = 160)
        String name,
        @NotBlank
        @Email
        @Size(max = 160)
        String email,
        @Size(max = 40)
        String phoneNumber,
        @NotNull
        EventOrganizerStatus status
) {
    public UpdateEventOrganizerCommand toCommand(UUID organizerId) {
        return new UpdateEventOrganizerCommand(
                organizerId,
                slug,
                name,
                email,
                phoneNumber,
                status
        );
    }
}
