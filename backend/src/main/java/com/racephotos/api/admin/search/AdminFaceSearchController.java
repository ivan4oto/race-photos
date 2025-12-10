package com.racephotos.api.admin.search;

import com.racephotos.api.search.dto.FaceSearchResponse;
import com.racephotos.service.search.AdminFaceSearchService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/admin/search", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminFaceSearchController {

    private final AdminFaceSearchService adminFaceSearchService;

    public AdminFaceSearchController(AdminFaceSearchService adminFaceSearchService) {
        this.adminFaceSearchService = adminFaceSearchService;
    }

    @PostMapping(path = "/events/{eventId}/faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FaceSearchResponse> uploadAndSearch(
            @PathVariable UUID eventId,
            @RequestPart("file") MultipartFile file
    ) {
        var result = adminFaceSearchService.uploadProbeAndSearch(eventId, file);
        return ResponseEntity.ok(FaceSearchResponse.from(result));
    }
}
