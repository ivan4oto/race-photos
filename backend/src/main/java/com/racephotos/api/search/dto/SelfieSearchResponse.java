package com.racephotos.api.search.dto;

import com.racephotos.service.search.FaceSearchService;

import java.util.List;

public record SelfieSearchResponse(
        String eventId,
        String probePhotoKey,
        List<Match> matches
) {
    public static SelfieSearchResponse from(FaceSearchService.FaceSearchResult result) {
        return new SelfieSearchResponse(
                result.eventId(),
                result.probePhotoKey(),
                result.matches().stream()
                        .map(m -> new Match(m.photoKey(), m.similarity()))
                        .toList()
        );
    }

    public record Match(String photoKey, float similarity) { }
}
