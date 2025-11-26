package com.racephotos.api.admin.participants;

import com.racephotos.api.admin.participants.dto.CreateParticipantProviderRequest;
import com.racephotos.api.admin.participants.dto.ParticipantProviderResponse;
import com.racephotos.auth.session.SessionUser;
import com.racephotos.domain.participant.ParticipantProvider;
import com.racephotos.service.participant.ParticipantProviderAdminService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping(path = "/api/admin/providers", produces = MediaType.APPLICATION_JSON_VALUE)
public class ParticipantProviderAdminController {

    private static final Logger log = LogManager.getLogger(ParticipantProviderAdminController.class);

    private final ParticipantProviderAdminService providerAdminService;

    public ParticipantProviderAdminController(ParticipantProviderAdminService providerAdminService) {
        this.providerAdminService = providerAdminService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createProvider(
            @AuthenticationPrincipal SessionUser user,
            @Valid @RequestBody CreateParticipantProviderRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ParticipantProvider created = providerAdminService.create(
                request.displayName(),
                request.email(),
                request.website()
        );
        log.info("User {} created participant provider {} ({})", user.email(), created.getId(), created.getDisplayName());
        return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(created.getId())
                        .toUri()
        ).build();
    }

    @GetMapping
    public ResponseEntity<List<ParticipantProviderResponse>> listProviders(
            @AuthenticationPrincipal SessionUser user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ParticipantProviderResponse> providers = providerAdminService.list()
                .stream()
                .map(ParticipantProviderResponse::from)
                .toList();
        return ResponseEntity.ok(providers);
    }
}
