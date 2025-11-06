package com.racephotos.api;

import com.racephotos.service.S3UrlService;
import com.racephotos.service.S3UrlService.UrlEntry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3UrlService s3UrlService;

    public S3Controller(S3UrlService s3UrlService) {
        this.s3UrlService = s3UrlService;
    }

    @PostMapping(value = "/presigned-urls", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UrlEntry> generatePresignedPutUrls(@RequestBody List<String> names) {
        return s3UrlService.createPresignedPutUrls(names);
    }
}

