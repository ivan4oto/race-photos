package com.racephotos.service.event;

import com.racephotos.api.event.AccessibleEventResponse;
import com.racephotos.auth.user.AccessGrantStatus;
import com.racephotos.auth.user.EventAccessGrantRepository;
import com.racephotos.domain.event.Event;
import com.racephotos.domain.event.EventRepository;
import com.racephotos.service.storage.S3UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class EventPublicRetrieveService {

    private static final Duration COVER_URL_TTL = Duration.ofMinutes(180);

    private final EventRepository eventRepository;
    private final EventAccessGrantRepository accessGrantRepository;
    private final S3UrlService s3UrlService;

    public EventPublicRetrieveService(
            EventRepository eventRepository,
            EventAccessGrantRepository accessGrantRepository,
            S3UrlService s3UrlService
    ) {
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        this.accessGrantRepository = Objects.requireNonNull(accessGrantRepository, "accessGrantRepository");
        this.s3UrlService = Objects.requireNonNull(s3UrlService, "s3UrlService");
    }

    public List<AccessibleEventResponse> listAccessibleEvents(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        List<UUID> accessibleIds = accessGrantRepository.findByUserIdAndStatus(userId, AccessGrantStatus.ACTIVE)
                .stream()
                .map(grant -> grant.getEventId())
                .toList();
        if (accessibleIds.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventRepository.findAllById(accessibleIds);
        return mapEvents(events);
    }

    public List<AccessibleEventResponse> mapEvents(Collection<Event> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        return events.stream()
                .sorted(Comparator.comparing(Event::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResponse)
                .toList();
    }

    private AccessibleEventResponse toResponse(Event event) {
        String coverUrl = s3UrlService.createPresignedGetUrl(event.getCoverImageKey(), COVER_URL_TTL);
        return AccessibleEventResponse.from(event, coverUrl);
    }
}
