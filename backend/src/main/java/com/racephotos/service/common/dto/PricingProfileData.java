package com.racephotos.service.common.dto;

import java.math.BigDecimal;

public record PricingProfileData(
        BigDecimal pricePerPhoto,
        BigDecimal bundlePrice,
        Integer bundleSize,
        String currencyCode
) {}
