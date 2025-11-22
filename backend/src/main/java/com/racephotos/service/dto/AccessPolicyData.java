package com.racephotos.service.dto;

import com.racephotos.domain.event.EligibilityMode;

public record AccessPolicyData(
        EligibilityMode mode,
        String provider,
        String configuration
) {}
