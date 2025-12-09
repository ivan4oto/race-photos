package com.racephotos.api.storage;

import com.racephotos.api.storage.dto.S3SignedUrlRequest;
import com.racephotos.api.storage.dto.S3SignedUrlResponse;
import com.racephotos.auth.session.SessionUser;
import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventRepository;
import com.racephotos.domain.photographer.Photographer;
import com.racephotos.domain.photographer.PhotographerRepository;
import com.racephotos.service.storage.S3UrlService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/s3", produces = MediaType.APPLICATION_JSON_VALUE)
public class S3Controller {
    private static final Logger log = LogManager.getLogger(S3Controller.class);

    private final S3UrlService s3UrlService;
    private final EventRepository eventRepository;
    private final PhotographerRepository photographerRepository;

    public S3Controller(
            S3UrlService s3UrlService,
            EventRepository eventRepository,
            PhotographerRepository photographerRepository
    ) {
        this.s3UrlService = s3UrlService;
        this.eventRepository = eventRepository;
        this.photographerRepository = photographerRepository;
    }

    @PostMapping(path = "/events/{eventSlug}/presigned-urls", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<S3SignedUrlResponse> generateSignedUrl(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable String eventSlug,
            @Valid @RequestBody S3SignedUrlRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Event event = eventRepository.findBySlug(eventSlug)
                .orElse(null);
        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Photographer photographer = photographerRepository.findByEmailIgnoreCase(user.email())
                .orElse(null);
        if (photographer == null) {
            log.warn("User {} attempted to upload photos for event {} but photographer does not exist for this email.", user.email(), eventSlug);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean allowed = event.getPhotographers() != null && event.getPhotographers()
                .stream()
                .anyMatch(p -> p.getId().equals(photographer.getId()));

        if (!allowed) {
            log.warn("User {} attempted to upload photos for event {} but is not a photographer for this event.", user.email(), eventSlug);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var urls = s3UrlService.createPresignedPutUrls(
                event.getSlug(),
                photographer.getSlug(),
                request.names(),
                request.folderName()
        );
        return ResponseEntity.ok(S3SignedUrlResponse.from(urls));
    }
}
