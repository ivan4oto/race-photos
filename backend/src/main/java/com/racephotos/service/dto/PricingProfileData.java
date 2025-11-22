package com.racephotos.service.dto;

import java.math.BigDecimal;

public record PricingProfileData(
        BigDecimal pricePerPhoto,
        BigDecimal bundlePrice,
        Integer bundleSize,
        String currencyCode
) {}
