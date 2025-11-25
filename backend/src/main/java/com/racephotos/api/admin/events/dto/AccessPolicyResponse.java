package com.racephotos.api.admin.events.dto;

import com.racephotos.domain.event.EventAccessPolicy;
import com.racephotos.domain.event.EligibilityMode;

public record AccessPolicyResponse(
        EligibilityMode mode,
        String provider,
        String configuration
) {
    public static AccessPolicyResponse from(EventAccessPolicy policy) {
        if (policy == null) {
            return new AccessPolicyResponse(EligibilityMode.NONE, null, null);
        }
        return new AccessPolicyResponse(
                policy.getMode(),
                policy.getProvider(),
                policy.getConfiguration()
        );
    }
}
