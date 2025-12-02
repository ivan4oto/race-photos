package com.racephotos.api.event;

import com.racephotos.auth.session.SessionUser;
import com.racephotos.auth.user.AccessGrantStatus;
import com.racephotos.auth.user.EventAccessGrantRepository;
import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/events", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventAccessController {

    private final EventRepository eventRepository;
    private final EventAccessGrantRepository accessGrantRepository;

    public EventAccessController(EventRepository eventRepository, EventAccessGrantRepository accessGrantRepository) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        this.accessGrantRepository = Objects.requireNonNull(accessGrantRepository, "accessGrantRepository");
    }

    @GetMapping
    public ResponseEntity<List<AccessibleEventResponse>> listAccessibleEvents(
            @AuthenticationPrincipal SessionUser user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<UUID> accessibleIds = accessGrantRepository.findByUserIdAndStatus(user.id(), AccessGrantStatus.ACTIVE)
                .stream()
                .map(grant -> grant.getEventId())
                .toList();
        if (accessibleIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<AccessibleEventResponse> responses = eventRepository.findAllById(accessibleIds).stream()
                .sorted(Comparator.comparing(Event::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(AccessibleEventResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }
}
