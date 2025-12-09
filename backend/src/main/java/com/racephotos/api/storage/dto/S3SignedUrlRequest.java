package com.racephotos.api.storage.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record S3SignedUrlRequest(
        @NotNull
        @NotEmpty
        @Size(max = 200)
        List<@Size(max = 255) String> names,
        @Size(max = 255)
        String folderName
) {
}
