package com.racephotos.api;

import com.racephotos.service.FaceIndexingService;
import com.racephotos.service.S3UrlService;
import com.racephotos.service.S3UrlService.UrlEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    private static final Logger log = LogManager.getLogger(S3Controller.class);

    private final S3UrlService s3UrlService;
    private final FaceIndexingService faceIndexingService;

    public S3Controller(S3UrlService s3UrlService, FaceIndexingService faceIndexingService) {
        this.s3UrlService = s3UrlService;
        this.faceIndexingService = faceIndexingService;
    }

    @PostMapping(value = "/presigned-urls", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UrlEntry> generatePresignedPutUrls(@RequestBody List<String> names) {
        log.info("Received presign request with {} object names", names == null ? 0 : names.size());
        List<UrlEntry> urls = s3UrlService.createPresignedPutUrls(names);
        log.info("Returning {} presigned URLs", urls.size());
        return urls;
    }

//    Todo: This is a test endpoint, we should move this to a upload based trigger
    @PostMapping(value = "/index-faces")
    public String indexFaces(@RequestBody List<String> objectKeys) {
        List<String> keys = new ArrayList<>();
        for (int i = 1; i < 292; i++) {
            keys.add("in/3/testgochev/raw/" + i + ".jpg");
        };
        faceIndexingService.indexFacesForEvent("3", keys);
        return "OK";
    }
}
