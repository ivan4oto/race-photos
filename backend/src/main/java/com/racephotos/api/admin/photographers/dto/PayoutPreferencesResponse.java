package com.racephotos.api.admin.photographers.dto;

import com.racephotos.domain.photographer.PayoutMethod;
import com.racephotos.domain.photographer.PayoutPreferences;

public record PayoutPreferencesResponse(
        PayoutMethod method,
        String accountReference,
        String payoutEmail,
        String bankAccountLast4,
        String bankRoutingNumber,
        String taxId,
        String metadata
) {
    public static PayoutPreferencesResponse from(PayoutPreferences preferences) {
        if (preferences == null) {
            return new PayoutPreferencesResponse(PayoutMethod.UNSPECIFIED, null, null, null, null, null, null);
        }
        return new PayoutPreferencesResponse(
                preferences.getMethod(),
                preferences.getAccountReference(),
                preferences.getPayoutEmail(),
                preferences.getBankAccountLast4(),
                preferences.getBankRoutingNumber(),
                preferences.getTaxId(),
                preferences.getMetadata()
        );
    }
}
