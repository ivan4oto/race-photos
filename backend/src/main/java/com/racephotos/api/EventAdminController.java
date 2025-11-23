package com.racephotos.api;

import com.racephotos.api.dto.CreateEventRequest;
import com.racephotos.domain.event.Event;
import com.racephotos.service.EventAdminService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.racephotos.auth.session.SessionUser;

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
}
