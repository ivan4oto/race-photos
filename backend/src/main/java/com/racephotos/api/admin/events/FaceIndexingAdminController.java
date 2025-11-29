package com.racephotos.api.admin.events;

import com.racephotos.auth.session.SessionUser;
import com.racephotos.service.ingestion.FaceIndexingJobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/admin/events", produces = MediaType.APPLICATION_JSON_VALUE)
public class FaceIndexingAdminController {

    private final FaceIndexingJobService faceIndexingJobService;

    public FaceIndexingAdminController(FaceIndexingJobService faceIndexingJobService) {
        this.faceIndexingJobService = faceIndexingJobService;
    }

    @PostMapping("/{eventId}/index-faces")
    public ResponseEntity<Void> triggerIndexing(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID eventId
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        faceIndexingJobService.indexUnindexedAsync(eventId);
        return ResponseEntity.accepted().build();
    }
}
