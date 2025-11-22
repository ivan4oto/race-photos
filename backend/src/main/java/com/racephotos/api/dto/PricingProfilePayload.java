package com.racephotos.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PricingProfilePayload(
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal pricePerPhoto,
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal bundlePrice,
        @Positive
        Integer bundleSize,
        @Size(max = 3)
        String currencyCode
) {}
