package com.racephotos.api.admin.events;

import com.racephotos.api.admin.events.dto.CreateEventOrganizerRequest;
import com.racephotos.api.admin.events.dto.EventOrganizerResponse;
import com.racephotos.api.admin.events.dto.UpdateEventOrganizerRequest;
import com.racephotos.domain.event.EventOrganizer;
import com.racephotos.service.event.EventOrganizerAdminService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/admin/organizers", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventOrganizerAdminController {

    private static final Logger log = LogManager.getLogger(EventOrganizerAdminController.class);

    private final EventOrganizerAdminService eventOrganizerAdminService;

    public EventOrganizerAdminController(EventOrganizerAdminService eventOrganizerAdminService) {
        this.eventOrganizerAdminService = eventOrganizerAdminService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventOrganizerResponse> createOrganizer(
            @Valid @RequestBody CreateEventOrganizerRequest request
    ) {
        EventOrganizer created = eventOrganizerAdminService.createOrganizer(request.toCommand());
        log.info("Created organizer {} with slug '{}'", created.getId(), created.getSlug());
        return ResponseEntity.created(
                        ServletUriComponentsBuilder.fromCurrentRequest()
                                .path("/{id}")
                                .buildAndExpand(created.getId())
                                .toUri())
                .body(EventOrganizerResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<List<EventOrganizerResponse>> listOrganizers() {
        List<EventOrganizerResponse> organizers = eventOrganizerAdminService.listOrganizers()
                .stream()
                .map(EventOrganizerResponse::from)
                .toList();
        return ResponseEntity.ok(organizers);
    }

    @GetMapping("/{organizerId}")
    public ResponseEntity<EventOrganizerResponse> getOrganizer(
            @PathVariable UUID organizerId
    ) {
        EventOrganizer organizer = eventOrganizerAdminService.getOrganizer(organizerId);
        return ResponseEntity.ok(EventOrganizerResponse.from(organizer));
    }

    @PatchMapping(path = "/{organizerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventOrganizerResponse> updateOrganizer(
            @PathVariable UUID organizerId,
            @Valid @RequestBody UpdateEventOrganizerRequest request
    ) {
        EventOrganizer updated = eventOrganizerAdminService.updateOrganizer(request.toCommand(organizerId));
        return ResponseEntity.ok(EventOrganizerResponse.from(updated));
    }

    @DeleteMapping("/{organizerId}")
    public ResponseEntity<Void> disableOrganizer(
            @PathVariable UUID organizerId
    ) {
        eventOrganizerAdminService.disableOrganizer(organizerId);
        return ResponseEntity.noContent().build();
    }
}
