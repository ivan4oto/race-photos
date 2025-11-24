package com.racephotos.api.admin.shared.dto;

import com.racephotos.service.dto.PricingProfileData;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PricingProfilePayload(
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal pricePerPhoto,
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal bundlePrice,
        @PositiveOrZero
        Integer bundleSize,
        @NotNull
        @Size(max = 3)
        String currencyCode
) {
    public PricingProfileData toData() {
        return new PricingProfileData(pricePerPhoto, bundlePrice, bundleSize, currencyCode);
    }
}
