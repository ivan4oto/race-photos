package com.racephotos.api.admin.shared.dto;

import com.racephotos.domain.common.PricingProfile;

import java.math.BigDecimal;

public record PricingProfileResponse(
        BigDecimal pricePerPhoto,
        BigDecimal bundlePrice,
        Integer bundleSize,
        String currencyCode
) {
    public static PricingProfileResponse from(PricingProfile profile) {
        if (profile == null) {
            return new PricingProfileResponse(null, null, null, null);
        }
        return new PricingProfileResponse(
                profile.getPricePerPhoto(),
                profile.getBundlePrice(),
                profile.getBundleSize(),
                profile.getCurrencyCode()
        );
    }
}
