package com.racephotos.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.racephotos.auth.config.CognitoProperties;
import com.racephotos.auth.service.validator.CognitoAudienceValidator;
import com.racephotos.auth.service.validator.CognitoTokenUseValidator;
import com.racephotos.auth.session.SessionUser;

@Service
public class CognitoTokenVerifier {

    private final JwtDecoder idTokenDecoder;

    public CognitoTokenVerifier(CognitoProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withJwkSetUri(properties.jwkSetUri().toString())
            .build();

        var withIssuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
        var withAudience = new CognitoAudienceValidator(properties.appClientId());
        var withTokenUse = new CognitoTokenUseValidator("id");
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience, withTokenUse));

        this.idTokenDecoder = decoder;
    }

    public SessionUser verifyIdToken(String token) {
        try {
            Jwt jwt = idTokenDecoder.decode(token);
            return new SessionUser(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name")
            );
        } catch (JwtException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Cognito ID token", ex);
        }
    }
}
