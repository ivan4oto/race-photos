package com.racephotos.api.admin.participants.dto;

import com.racephotos.domain.participant.ParticipantProvider;

import java.time.Instant;
import java.util.UUID;

public record ParticipantProviderResponse(
        UUID id,
        String displayName,
        String email,
        String website,
        Instant createdAt,
        Instant updatedAt
) {
    public static ParticipantProviderResponse from(ParticipantProvider provider) {
        return new ParticipantProviderResponse(
                provider.getId(),
                provider.getDisplayName(),
                provider.getEmail(),
                provider.getWebsite(),
                provider.getCreatedAt(),
                provider.getUpdatedAt()
        );
    }
}
