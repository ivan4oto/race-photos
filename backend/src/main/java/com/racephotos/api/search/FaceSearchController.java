package com.racephotos.api.search;

import com.racephotos.api.search.dto.FaceSearchRequest;
import com.racephotos.api.search.dto.FaceSearchResponse;
import com.racephotos.auth.session.SessionUser;
import com.racephotos.service.FaceSearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/search", produces = MediaType.APPLICATION_JSON_VALUE)
public class FaceSearchController {

    private final FaceSearchService faceSearchService;

    public FaceSearchController(FaceSearchService faceSearchService) {
        this.faceSearchService = faceSearchService;
    }

    @PostMapping(path = "/faces", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FaceSearchResponse> search(
            @AuthenticationPrincipal SessionUser user,
            @Valid @RequestBody FaceSearchRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var result = faceSearchService.searchFaces(request.eventId(), request.photoKey());
        return ResponseEntity.ok(FaceSearchResponse.from(result));
    }
}
