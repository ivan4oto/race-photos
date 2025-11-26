package com.racephotos.api.admin.participants;

import com.racephotos.api.admin.participants.dto.ParticipantIngestRequest;
import com.racephotos.api.admin.participants.dto.ParticipantIngestResponse;
import com.racephotos.api.admin.participants.dto.ParticipantRegistrationPayload;
import com.racephotos.auth.session.SessionUser;
import com.racephotos.service.participant.ParticipantAdminService;
import com.racephotos.service.participant.dto.ParticipantRegistration;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/admin/events/{eventId}/participants", produces = MediaType.APPLICATION_JSON_VALUE)
public class ParticipantAdminController {

    private static final Logger log = LogManager.getLogger(ParticipantAdminController.class);

    private final ParticipantAdminService participantAdminService;

    public ParticipantAdminController(ParticipantAdminService participantAdminService) {
        this.participantAdminService = participantAdminService;
    }

    @PostMapping(path = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParticipantIngestResponse> importParticipants(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable UUID eventId,
            @Valid @RequestBody ParticipantIngestRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ParticipantRegistration> regs = request.registrations()
                .stream()
                .map(ParticipantAdminController::toRegistration)
                .toList();

        var result = participantAdminService.ingest(eventId, regs);
        log.info("User {} imported participants for event {} (created={}, unchanged={})",
                user.email(), eventId, result.createdCount(), result.unchangedCount());
        var outcomes = result.outcomes().stream()
                .map(o -> new ParticipantIngestResponse.RegistrationOutcome(
                        o.status().name(),
                        o.externalRegistrationId(),
                        o.email(),
                        o.firstName(),
                        o.lastName()
                ))
                .toList();
        return ResponseEntity.ok(ParticipantIngestResponse.of(result.createdCount(), result.unchangedCount(), outcomes));
    }

    private static ParticipantRegistration toRegistration(ParticipantRegistrationPayload payload) {
        return new ParticipantRegistration(
                payload.firstName(),
                payload.lastName(),
                payload.email(),
                payload.providerId(),
                payload.externalRegistrationId()
        );
    }
}
