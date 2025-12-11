package com.racephotos.api.admin.storage;

import com.racephotos.api.admin.storage.dto.DeleteByPrefixRequest;
import com.racephotos.api.admin.storage.dto.DeleteByPrefixResponse;
import com.racephotos.service.storage.AdminStorageService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/admin/storage", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminStorageController {

    private final AdminStorageService adminStorageService;

    public AdminStorageController(AdminStorageService adminStorageService) {
        this.adminStorageService = adminStorageService;
    }

    @PostMapping(path = "/delete-by-prefix", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeleteByPrefixResponse> deleteByPrefix(
            @Valid @RequestBody DeleteByPrefixRequest request
    ) {
        var result = adminStorageService.deleteByPrefix(request.prefix());
        return ResponseEntity.ok(DeleteByPrefixResponse.from(result.deletedS3Objects(), result.deletedPhotoAssets()));
    }
}
