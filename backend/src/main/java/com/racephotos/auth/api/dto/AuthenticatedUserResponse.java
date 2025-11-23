package com.racephotos.auth.api.dto;

import java.util.UUID;

import com.racephotos.auth.session.SessionUser;

public record AuthenticatedUserResponse(
    UUID id,
    String email,
    String sub,
    String givenName,
    String familyName,
    String profilePictureUrl
) {
    public static AuthenticatedUserResponse from(SessionUser user) {
        return new AuthenticatedUserResponse(
            user.id(),
            user.email(),
            user.sub(),
            user.givenName(),
            user.familyName(),
            user.profilePictureUrl()
        );
    }
}
