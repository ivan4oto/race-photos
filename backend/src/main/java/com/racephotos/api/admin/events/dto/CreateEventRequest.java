package com.racephotos.api.admin.events.dto;

import com.racephotos.api.admin.shared.dto.PricingProfilePayload;
import com.racephotos.domain.event.EventStatus;
import com.racephotos.service.event.dto.CreateEventCommand;
import com.racephotos.service.common.dto.PricingProfileData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateEventRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
        @Size(max = 80)
        String slug,
        @NotBlank
        @Size(max = 160)
        String name,
        @Size(max = 4000)
        String description,
        @NotNull
        EventStatus status,
        @Size(max = 160)
        String organizerName,
        @Size(max = 160)
        String registrationProvider,
        @Size(max = 160)
        String vectorCollectionId,
        @Size(max = 255)
        String uploadPrefix,
        @Size(max = 60)
        String timezone,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        @Size(max = 160)
        String locationName,
        @Size(max = 120)
        String locationCity,
        @Size(max = 120)
        String locationState,
        @Size(max = 120)
        String locationCountry,
        @Size(max = 255)
        String coverImageKey,
        @DecimalMin(value = "0.0", inclusive = true)
        @DecimalMax(value = "1.0", inclusive = true)
        BigDecimal platformCommissionRate,
        @NotNull
        Boolean watermarkingEnabled,
        @NotNull
        Boolean autoPublishMatches,
        @Valid
        @NotNull
        PricingProfilePayload defaultPricing,
        @Valid
        @NotNull
        AccessPolicyPayload accessPolicy,
        @Size(max = 4000)
        String participantMessage
) {
    public CreateEventCommand toCommand() {
        boolean watermarking = Boolean.TRUE.equals(watermarkingEnabled);
        boolean autoPublish = Boolean.TRUE.equals(autoPublishMatches);

        return new CreateEventCommand(
                slug,
                name,
                description,
                status,
                organizerName,
                registrationProvider,
                vectorCollectionId,
                uploadPrefix,
                timezone,
                startTime,
                endTime,
                locationName,
                locationCity,
                locationState,
                locationCountry,
                coverImageKey,
                platformCommissionRate,
                watermarking,
                autoPublish,
                new PricingProfileData(
                        defaultPricing.pricePerPhoto(),
                        defaultPricing.bundlePrice(),
                        defaultPricing.bundleSize(),
                        defaultPricing.currencyCode()
                ),
                accessPolicy.toData(),
                participantMessage
        );
    }
}
