package com.racephotos.auth.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "cognito")
public record CognitoProperties(
    @NotBlank String region,
    @NotBlank String userPoolId,
    @NotBlank String appClientId,
    Duration jwkCacheTtl
) {
    private static final String ISSUER_TEMPLATE = "https://cognito-idp.%s.amazonaws.com/%s";

    public String issuer() {
        return ISSUER_TEMPLATE.formatted(region, userPoolId);
    }

    public URI jwkSetUri() {
        return URI.create(issuer() + "/.well-known/jwks.json");
    }

    public Duration jwkCacheTtl() {
        return jwkCacheTtl != null ? jwkCacheTtl : Duration.ofMinutes(30);
    }
}
