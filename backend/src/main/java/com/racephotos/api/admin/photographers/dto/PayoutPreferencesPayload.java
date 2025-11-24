package com.racephotos.api.admin.photographers.dto;

import com.racephotos.domain.photographer.PayoutMethod;
import com.racephotos.service.dto.PayoutPreferencesData;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PayoutPreferencesPayload(
        @NotNull
        PayoutMethod method,
        @Size(max = 255)
        String accountReference,
        @Email
        @Size(max = 160)
        String payoutEmail,
        @Size(max = 4)
        String bankAccountLast4,
        @Size(max = 9)
        String bankRoutingNumber,
        @Size(max = 64)
        String taxId,
        @Size(max = 2000)
        String metadata
) {
    public PayoutPreferencesData toData() {
        return new PayoutPreferencesData(
                method,
                accountReference,
                payoutEmail,
                bankAccountLast4,
                bankRoutingNumber,
                taxId,
                metadata
        );
    }
}
