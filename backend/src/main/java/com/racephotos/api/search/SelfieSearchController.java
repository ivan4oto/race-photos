package com.racephotos.api.search;

import com.racephotos.api.search.dto.SelfieSearchResponse;
import com.racephotos.auth.session.SessionUser;
import com.racephotos.auth.user.AccessGrantStatus;
import com.racephotos.auth.user.EventAccessGrantRepository;
import com.racephotos.auth.user.UserSelfie;
import com.racephotos.auth.user.UserSelfieRepository;
import com.racephotos.service.search.FaceSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/{eventId}/search", produces = MediaType.APPLICATION_JSON_VALUE)
public class SelfieSearchController {

    private final FaceSearchService faceSearchService;
    private final EventAccessGrantRepository accessGrantRepository;
    private final UserSelfieRepository userSelfieRepository;

    public SelfieSearchController(
            FaceSearchService faceSearchService,
            EventAccessGrantRepository accessGrantRepository,
            UserSelfieRepository userSelfieRepository
    ) {
        this.faceSearchService = Objects.requireNonNull(faceSearchService, "faceSearchService");
        this.accessGrantRepository = Objects.requireNonNull(accessGrantRepository, "accessGrantRepository");
        this.userSelfieRepository = Objects.requireNonNull(userSelfieRepository, "userSelfieRepository");
    }

    @GetMapping
    public ResponseEntity<SelfieSearchResponse> searchWithSelfie(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID eventId
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean hasAccess = accessGrantRepository.findByUserIdAndStatus(user.id(), AccessGrantStatus.ACTIVE)
                .stream()
                .anyMatch(grant -> eventId.equals(grant.getEventId()));
        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserSelfie selfie = userSelfieRepository.findByUserId(user.id())
                .orElse(null);
        if (selfie == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        var result = faceSearchService.searchFaces(eventId.toString(), selfie.getS3Key());
        return ResponseEntity.ok(SelfieSearchResponse.from(result));
    }
}
