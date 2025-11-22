package com.racephotos.api.dto;

import com.racephotos.domain.event.EligibilityMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccessPolicyPayload(
        @NotNull
        EligibilityMode mode,
        @Size(max = 120)
        String provider,
        @Size(max = 4000)
        String configuration
) {}
