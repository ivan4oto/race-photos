package com.racephotos.auth.api.dto;

import com.racephotos.auth.session.SessionUser;

public record AuthenticatedUserResponse(
    String email,
    String sub,
    String givenName,
    String familyName
) {
    public static AuthenticatedUserResponse from(SessionUser user) {
        return new AuthenticatedUserResponse(
            user.email(),
            user.sub(),
            user.givenName(),
            user.familyName()
        );
    }
}
