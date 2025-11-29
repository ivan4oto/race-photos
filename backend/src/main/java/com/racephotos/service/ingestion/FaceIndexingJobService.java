package com.racephotos.service.ingestion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.Future;

@Service
public class FaceIndexingJobService {

    private static final Logger log = LogManager.getLogger(FaceIndexingJobService.class);

    private final FaceIndexingService faceIndexingService;

    public FaceIndexingJobService(FaceIndexingService faceIndexingService) {
        this.faceIndexingService = faceIndexingService;
    }

    @Async("faceIndexingExecutor")
    public Future<Void> indexUnindexedAsync(UUID eventId) {
        try {
            var report = faceIndexingService.indexUnindexedPhotoAssets(eventId);
            log.info("Async indexing complete for event {} (requested={}, success={}, failed={})",
                    eventId, report.requestedImages(), report.successfullyIndexedImages(), report.failedImages().size());
        } catch (Exception e) {
            log.error("Async indexing failed for event {}", eventId, e);
        }
        return AsyncResult.forValue(null);
    }
}
