package com.racephotos.api.admin.events;

import com.racephotos.api.admin.events.dto.AddPhotographerToEventRequest;
import com.racephotos.api.admin.events.dto.CreateEventRequest;
import com.racephotos.api.admin.events.dto.EventDetailResponse;
import com.racephotos.api.admin.events.dto.EventSummaryResponse;
import com.racephotos.api.admin.events.dto.UpdateEventRequest;
import com.racephotos.auth.session.SessionUser;
import com.racephotos.domain.event.Event;
import com.racephotos.service.event.EventAdminService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/admin/events", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventAdminController {

    private static final Logger log = LogManager.getLogger(EventAdminController.class);

    private final EventAdminService eventAdminService;

    public EventAdminController(EventAdminService eventAdminService) {
        this.eventAdminService = eventAdminService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createEvent(
            @AuthenticationPrincipal SessionUser user,
            @Valid @RequestBody CreateEventRequest request
    ) {
        // TODO: pass user as author in the future.
        String userEmail = user == null ? "<anonymous>" : user.email();
        log.info("User {} requested event creation with slug '{}'", userEmail, request.slug());

        Event created = eventAdminService.createEvent(request.toCommand());
        return ResponseEntity.created(
                        ServletUriComponentsBuilder.fromCurrentRequest()
                                .path("/{id}")
                                .buildAndExpand(created.getId())
                                .toUri())
                .build();
    }

    @GetMapping
    public ResponseEntity<List<EventSummaryResponse>> listEvents() {
        List<EventSummaryResponse> events = eventAdminService.listEvents()
                .stream()
                .map(EventSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> getEvent(
            @PathVariable UUID eventId
    ) {
        Event event = eventAdminService.getEvent(eventId);
        return ResponseEntity.ok(toDetailResponse(event));
    }

    // Returns a count for how many files are there for each 'folder' in S3
    // Uses delimiter to figure out directories and counts keys for each
    // Used for admin audit and photos clean up in case of issues.
    @GetMapping("/{eventId}/photo-prefix-counts")
    public ResponseEntity<Map<String, Long>> getPhotoPrefixCounts(
            @PathVariable UUID eventId
    ) {
        Map<String, Long> counts = eventAdminService.getPhotoPrefixCounts(eventId);
        return ResponseEntity.ok(counts);
    }

    @PutMapping(path = "/{eventId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventDetailResponse> updateEvent(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        // TODO: use user when auditing updates.
        Event updated = eventAdminService.updateEvent(request.toCommand(eventId));
        return ResponseEntity.ok(toDetailResponse(updated));
    }

    @PostMapping(path = "/{eventId}/photographers", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventDetailResponse> addPhotographer(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID eventId,
            @Valid @RequestBody AddPhotographerToEventRequest request
    ) {
        Event updated = eventAdminService.addPhotographer(eventId, request.toCommand());
        return ResponseEntity.ok(toDetailResponse(updated));
    }

    @DeleteMapping("/{eventId}/photographers/{photographerId}")
    public ResponseEntity<Void> removePhotographer(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID eventId,
            @PathVariable UUID photographerId
    ) {
        eventAdminService.removePhotographer(eventId, photographerId);
        return ResponseEntity.noContent().build();
    }

    private EventDetailResponse toDetailResponse(Event event) {
        var summary = eventAdminService.getPhotoAssetSummary(event.getId());
        return EventDetailResponse.from(event, summary.indexedPhotoCount(), summary.unindexedPhotoCount());
    }
}
