package com.racephotos.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/**
 * Simple embeddable type representing the most common per-photo and bundle pricing knobs.
 */
@Embeddable
public class PricingProfile {

    private static final int DEFAULT_PRECISION = 10;
    private static final int DEFAULT_SCALE = 2;

    @Column(name = "price_per_photo", precision = DEFAULT_PRECISION, scale = DEFAULT_SCALE)
    private BigDecimal pricePerPhoto;

    @Column(name = "bundle_price", precision = DEFAULT_PRECISION, scale = DEFAULT_SCALE)
    private BigDecimal bundlePrice;

    @Column(name = "bundle_size")
    private Integer bundleSize;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    public PricingProfile() {
    }

    public BigDecimal getPricePerPhoto() {
        return pricePerPhoto;
    }

    public void setPricePerPhoto(BigDecimal pricePerPhoto) {
        this.pricePerPhoto = pricePerPhoto;
    }

    public BigDecimal getBundlePrice() {
        return bundlePrice;
    }

    public void setBundlePrice(BigDecimal bundlePrice) {
        this.bundlePrice = bundlePrice;
    }

    public Integer getBundleSize() {
        return bundleSize;
    }

    public void setBundleSize(Integer bundleSize) {
        this.bundleSize = bundleSize;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
