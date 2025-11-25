package com.racephotos.service.participant.dto;

public record ParticipantIngestResult(
        int createdCount,
        int unchangedCount
) {
    public static ParticipantIngestResult of(int created, int unchanged) {
        return new ParticipantIngestResult(created, unchanged);
    }
}
