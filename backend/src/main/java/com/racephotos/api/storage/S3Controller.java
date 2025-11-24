package com.racephotos.api.storage;

import com.racephotos.api.storage.dto.S3SignedUrlRequest;
import com.racephotos.api.storage.dto.S3SignedUrlResponse;
import com.racephotos.service.S3UrlService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/s3", produces = MediaType.APPLICATION_JSON_VALUE)
public class S3Controller {

    private final S3UrlService s3UrlService;

    public S3Controller(S3UrlService s3UrlService) {
        this.s3UrlService = s3UrlService;
    }

    @PostMapping(path = "/sign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<S3SignedUrlResponse> generateSignedUrl(
            @Valid @RequestBody S3SignedUrlRequest request
    ) {
        var urls = s3UrlService.createPresignedPutUrls(request.names());
        return ResponseEntity.ok(S3SignedUrlResponse.from(urls));
    }
}
