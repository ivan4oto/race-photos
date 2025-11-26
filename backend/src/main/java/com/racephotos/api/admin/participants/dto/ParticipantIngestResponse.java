package com.racephotos.api.admin.participants.dto;

import java.util.List;

public record ParticipantIngestResponse(
        int createdCount,
        int unchangedCount,
        List<RegistrationOutcome> outcomes
) {
    public static ParticipantIngestResponse of(
            int created,
            int unchanged,
            List<RegistrationOutcome> outcomes
    ) {
        return new ParticipantIngestResponse(created, unchanged, outcomes);
    }

    public record RegistrationOutcome(
            String status,
            String externalRegistrationId,
            String email,
            String firstName,
            String lastName
    ) {
    }
}
