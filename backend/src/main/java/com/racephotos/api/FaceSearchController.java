package com.racephotos.api;

import com.racephotos.service.FaceSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/faces", produces = MediaType.APPLICATION_JSON_VALUE)
public class FaceSearchController {

    private final FaceSearchService faceSearchService;

    public FaceSearchController(FaceSearchService faceSearchService) {
        this.faceSearchService = faceSearchService;
    }

    @PostMapping(path = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FaceSearchResponse searchFaces(@Valid @RequestBody FaceSearchRequest request) {
        FaceSearchService.FaceSearchResult result = faceSearchService.searchFaces(request.eventId(), request.photoKey());
        List<MatchResponse> matches = result.matches().stream()
                .map(match -> new MatchResponse(
                        match.photoKey(),
                        match.faceId(),
                        match.similarity(),
                        match.confidence(),
                        FaceBoundingBox.from(match.boundingBox())
                ))
                .toList();

        return new FaceSearchResponse(result.eventId(), result.probePhotoKey(), matches);
    }

    public record FaceSearchRequest(
            @NotBlank String eventId,
            @NotBlank String photoKey
    ) {}

    public record FaceSearchResponse(
            String eventId,
            String queryPhotoKey,
            List<MatchResponse> matches
    ) {}

    public record MatchResponse(
            String photoKey,
            String faceId,
            float similarity,
            Float confidence,
            FaceBoundingBox boundingBox
    ) {}

    public record FaceBoundingBox(Float width, Float height, Float left, Float top) {
        static FaceBoundingBox from(software.amazon.awssdk.services.rekognition.model.BoundingBox bbox) {
            if (bbox == null) {
                return null;
            }
            return new FaceBoundingBox(bbox.width(), bbox.height(), bbox.left(), bbox.top());
        }
    }
}
