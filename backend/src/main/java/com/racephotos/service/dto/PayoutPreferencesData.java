package com.racephotos.service.dto;

import com.racephotos.domain.photographer.PayoutMethod;

public record PayoutPreferencesData(
        PayoutMethod method,
        String accountReference,
        String payoutEmail,
        String bankAccountLast4,
        String bankRoutingNumber,
        String taxId,
        String metadata
) {
}
