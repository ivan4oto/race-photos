package com.racephotos.service.participant.dto;

import java.util.List;

public record ParticipantIngestResult(
        int createdCount,
        int unchangedCount,
        List<RegistrationOutcome> outcomes
) {
    public static ParticipantIngestResult of(int created, int unchanged, List<RegistrationOutcome> outcomes) {
        return new ParticipantIngestResult(created, unchanged, outcomes);
    }

    public enum RegistrationStatus {
        CREATED,
        UNCHANGED,
        SKIPPED_INVALID
    }

    public record RegistrationOutcome(
            RegistrationStatus status,
            String externalRegistrationId,
            String email,
            String firstName,
            String lastName
    ) {
    }
}
