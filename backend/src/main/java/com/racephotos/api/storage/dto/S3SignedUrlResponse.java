package com.racephotos.api.storage.dto;

import com.racephotos.service.S3UrlService;

import java.util.List;

public record S3SignedUrlResponse(
        List<UrlEntry> urls
) {
    public static S3SignedUrlResponse from(List<S3UrlService.UrlEntry> entries) {
        return new S3SignedUrlResponse(entries.stream()
                .map(entry -> new UrlEntry(entry.name(), entry.url()))
                .toList());
    }

    public record UrlEntry(String name, String url) {}
}
