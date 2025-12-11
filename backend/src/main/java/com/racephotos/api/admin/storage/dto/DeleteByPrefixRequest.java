package com.racephotos.api.admin.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteByPrefixRequest(
        @NotBlank
        @Size(max = 1024)
        String prefix
) { }
