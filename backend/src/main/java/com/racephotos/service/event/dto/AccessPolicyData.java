package com.racephotos.service.event.dto;

import com.racephotos.domain.event.EligibilityMode;

public record AccessPolicyData(
        EligibilityMode mode,
        String provider,
        String configuration
) {}
