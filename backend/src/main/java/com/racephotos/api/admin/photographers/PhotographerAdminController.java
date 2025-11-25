package com.racephotos.api.admin.photographers;

import com.racephotos.api.admin.photographers.dto.CreatePhotographerRequest;
import com.racephotos.api.admin.photographers.dto.PhotographerDetailResponse;
import com.racephotos.api.admin.photographers.dto.PhotographerSummaryResponse;
import com.racephotos.api.admin.photographers.dto.UpdatePhotographerRequest;
import com.racephotos.auth.session.SessionUser;
import com.racephotos.service.photographer.PhotographerAdminService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/admin/photographers", produces = MediaType.APPLICATION_JSON_VALUE)
public class PhotographerAdminController {

    private static final Logger log = LogManager.getLogger(PhotographerAdminController.class);

    private final PhotographerAdminService photographerAdminService;

    public PhotographerAdminController(PhotographerAdminService photographerAdminService) {
        this.photographerAdminService = photographerAdminService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createPhotographer(
            @AuthenticationPrincipal SessionUser user,
            @Valid @RequestBody CreatePhotographerRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("User {} requested photographer creation with slug '{}'", user.email(), request.slug());

        var created = photographerAdminService.createPhotographer(request.toCommand(user.id()));
        return ResponseEntity.created(
                        ServletUriComponentsBuilder.fromCurrentRequest()
                                .path("/{id}")
                                .buildAndExpand(created.getId())
                                .toUri())
                .build();
    }

    @GetMapping
    public ResponseEntity<List<PhotographerSummaryResponse>> listPhotographers() {
        List<PhotographerSummaryResponse> response = photographerAdminService.listPhotographers()
                .stream()
                .map(PhotographerSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{photographerId}")
    public ResponseEntity<PhotographerDetailResponse> getPhotographer(
            @PathVariable UUID photographerId
    ) {
        var photographer = photographerAdminService.getPhotographer(photographerId);
        return ResponseEntity.ok(PhotographerDetailResponse.from(photographer));
    }

    @PutMapping(path = "/{photographerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PhotographerDetailResponse> updatePhotographer(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID photographerId,
            @Valid @RequestBody UpdatePhotographerRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("User {} updating photographer {}", user.email(), photographerId);
        var updated = photographerAdminService.updatePhotographer(request.toCommand(photographerId));
        return ResponseEntity.ok(PhotographerDetailResponse.from(updated));
    }
}
