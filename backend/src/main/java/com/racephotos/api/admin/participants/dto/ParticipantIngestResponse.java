package com.racephotos.api.admin.participants.dto;

public record ParticipantIngestResponse(
        int createdCount,
        int unchangedCount
) {
    public static ParticipantIngestResponse of(int created, int unchanged) {
        return new ParticipantIngestResponse(created, unchanged);
    }
}
