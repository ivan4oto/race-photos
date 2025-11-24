package com.racephotos.api.search.dto;

import com.racephotos.service.FaceSearchService;
import software.amazon.awssdk.services.rekognition.model.BoundingBox;

import java.util.List;

public record FaceSearchResponse(
        String eventId,
        String probePhotoKey,
        List<Match> matches
) {
    public static FaceSearchResponse from(FaceSearchService.FaceSearchResult result) {
        return new FaceSearchResponse(
                result.eventId(),
                result.probePhotoKey(),
                result.matches()
                        .stream()
                        .map(match -> new Match(
                                match.photoKey(),
                                match.faceId(),
                                match.similarity(),
                                match.confidence(),
                                BoundingBoxPayload.from(match.boundingBox())
                        ))
                        .toList()
        );
    }

    public record Match(
            String photoKey,
            String faceId,
            float similarity,
            Float confidence,
            BoundingBoxPayload boundingBox
    ) {
    }

    public record BoundingBoxPayload(Float left, Float top, Float width, Float height) {
        public static BoundingBoxPayload from(BoundingBox box) {
            if (box == null) {
                return new BoundingBoxPayload(null, null, null, null);
            }
            return new BoundingBoxPayload(box.left(), box.top(), box.width(), box.height());
        }
    }
}
