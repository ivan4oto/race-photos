package com.racephotos.auth.session;

import com.racephotos.auth.user.User;

import java.util.UUID;

public record SessionUser(
    UUID id,
    String sub,
    String email,
    String givenName,
    String familyName,
    String profilePictureUrl
) {
    public static SessionUser from(User user) {
        if (user == null) {
            return null;
        }
        return new SessionUser(
            user.getId(),
            user.getCognitoSub(),
            user.getEmail(),
            user.getFirstName(),
            user.getFamilyName(),
            user.getProfilePictureUrl()
        );
    }
}
