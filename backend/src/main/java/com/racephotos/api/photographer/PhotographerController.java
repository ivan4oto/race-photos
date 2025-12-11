package com.racephotos.api.photographer;

import com.racephotos.api.event.AccessibleEventResponse;
import com.racephotos.auth.session.SessionUser;
import com.racephotos.auth.user.Role;
import com.racephotos.domain.photographer.Photographer;
import com.racephotos.domain.photographer.PhotographerRepository;
import com.racephotos.service.event.EventPublicRetrieveService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(path = "/api/photographer", produces = MediaType.APPLICATION_JSON_VALUE)
public class PhotographerController {

    private final PhotographerRepository photographerRepository;
    private final EventPublicRetrieveService eventPublicRetrieveService;

    public PhotographerController(
            PhotographerRepository photographerRepository,
            EventPublicRetrieveService eventPublicRetrieveService
    ) {
        this.photographerRepository = Objects.requireNonNull(photographerRepository, "photographerRepository");
        this.eventPublicRetrieveService = Objects.requireNonNull(eventPublicRetrieveService, "eventPublicRetrieveService");
    }

    @GetMapping("/events")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AccessibleEventResponse>> listPhotographerEvents(
            @AuthenticationPrincipal SessionUser user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (user.email() == null || user.email().isBlank() || !user.roles().contains(Role.PHOTOGRAPHER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Photographer photographer = photographerRepository.findByEmailIgnoreCase(user.email())
                .orElse(null);
        if (photographer == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<AccessibleEventResponse> events = eventPublicRetrieveService.mapEvents(photographer.getEvents());

        return ResponseEntity.ok(events);
    }
}
