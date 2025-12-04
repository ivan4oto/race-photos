package com.racephotos.api.admin.selfie;

import com.racephotos.service.selfie.SelfieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/admin/selfies")
public class SelfieAdminController {

    private final SelfieService selfieService;

    public SelfieAdminController(SelfieService selfieService) {
        this.selfieService = selfieService;
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteSelfie(@PathVariable UUID userId) {
        selfieService.deleteSelfie(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
