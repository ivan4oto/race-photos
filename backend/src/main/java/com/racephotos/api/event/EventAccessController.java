package com.racephotos.api.event;

import com.racephotos.auth.session.SessionUser;
import com.racephotos.service.event.EventPublicRetrieveService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(path = "/api/events", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventAccessController {

    private final EventPublicRetrieveService eventPublicRetrieveService;

    public EventAccessController(EventPublicRetrieveService eventPublicRetrieveService) {
        this.eventPublicRetrieveService = Objects.requireNonNull(eventPublicRetrieveService, "eventPublicRetrieveService");
    }

    @GetMapping
    public ResponseEntity<List<AccessibleEventResponse>> listAccessibleEvents(
            @AuthenticationPrincipal SessionUser user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<AccessibleEventResponse> responses = eventPublicRetrieveService.listAccessibleEvents(user.id());
        return ResponseEntity.ok(responses);
    }
}
