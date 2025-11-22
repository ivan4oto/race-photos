package com.racephotos.api;

import com.racephotos.api.dto.CreateEventRequest;
import com.racephotos.domain.event.Event;
import com.racephotos.service.EventAdminService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/admin/events", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventAdminController {

    private final EventAdminService eventAdminService;

    public EventAdminController(EventAdminService eventAdminService) {
        this.eventAdminService = eventAdminService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createEvent(@Valid @RequestBody CreateEventRequest request) {
        Event created = eventAdminService.createEvent(request.toCommand());
        return ResponseEntity.created(
                        ServletUriComponentsBuilder.fromCurrentRequest()
                                .path("/{id}")
                                .buildAndExpand(created.getId())
                                .toUri())
                .build();
    }
}
