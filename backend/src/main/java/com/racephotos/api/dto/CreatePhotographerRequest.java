package com.racephotos.api.dto;

import com.racephotos.domain.photographer.PhotographerStatus;
import com.racephotos.service.dto.CreatePhotographerCommand;
import com.racephotos.service.dto.PricingProfileData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePhotographerRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
        @Size(max = 80)
        String slug,
        @NotBlank
        @Size(max = 80)
        String firstName,
        @NotBlank
        @Size(max = 80)
        String lastName,
        @NotBlank
        @Size(max = 160)
        String displayName,
        @NotBlank
        @Email
        @Size(max = 160)
        String email,
        @Size(max = 40)
        String phoneNumber,
        @Size(max = 160)
        String studioName,
        @Size(max = 255)
        String website,
        @Size(max = 3)
        String defaultCurrency,
        @NotNull
        PhotographerStatus status,
        @Size(max = 4000)
        String biography,
        @DecimalMin(value = "0.0", inclusive = true)
        @DecimalMax(value = "1.0", inclusive = true)
        BigDecimal commissionOverride,
        @Valid
        @NotNull
        PricingProfilePayload rateCard,
        @Valid
        @NotNull
        PayoutPreferencesPayload payoutPreferences,
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal payoutThreshold,
        @Size(max = 4000)
        String internalNotes
) {
    public CreatePhotographerCommand toCommand(UUID createdByUserId) {
        return new CreatePhotographerCommand(
                slug,
                firstName,
                lastName,
                displayName,
                email,
                phoneNumber,
                studioName,
                website,
                defaultCurrency,
                status,
                biography,
                commissionOverride,
                new PricingProfileData(
                        rateCard.pricePerPhoto(),
                        rateCard.bundlePrice(),
                        rateCard.bundleSize(),
                        rateCard.currencyCode()
                ),
                payoutPreferences.toData(),
                payoutThreshold,
                internalNotes,
                createdByUserId
        );
    }
}
