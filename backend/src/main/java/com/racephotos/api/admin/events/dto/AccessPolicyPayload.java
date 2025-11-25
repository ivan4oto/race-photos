package com.racephotos.api.admin.events.dto;

import com.racephotos.domain.event.EligibilityMode;
import com.racephotos.service.event.dto.AccessPolicyData;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccessPolicyPayload(
        @NotNull
        EligibilityMode mode,
        @Size(max = 160)
        String provider,
        @Size(max = 4000)
        String configuration
) {
    public AccessPolicyData toData() {
        return new AccessPolicyData(mode, provider, configuration);
    }
}
