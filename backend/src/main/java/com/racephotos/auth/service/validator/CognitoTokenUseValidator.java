package com.racephotos.auth.service.validator;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class CognitoTokenUseValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedTokenUse;

    public CognitoTokenUseValidator(String expectedTokenUse) {
        this.expectedTokenUse = expectedTokenUse;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String tokenUse = token.getClaimAsString("token_use");
        if (expectedTokenUse.equals(tokenUse)) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error("invalid_token", "Cognito token has incorrect token_use claim.", null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
