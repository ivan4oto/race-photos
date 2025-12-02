package com.racephotos.api.selfie;

import com.racephotos.auth.session.SessionUser;
import com.racephotos.service.selfie.SelfieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/api/selfie", produces = MediaType.APPLICATION_JSON_VALUE)
public class SelfieController {

    private final SelfieService selfieService;

    public SelfieController(SelfieService selfieService) {
        this.selfieService = selfieService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadSelfie(
            @AuthenticationPrincipal SessionUser user,
            @RequestPart("file") MultipartFile file
    ) {
        selfieService.uploadSelfie(user, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
