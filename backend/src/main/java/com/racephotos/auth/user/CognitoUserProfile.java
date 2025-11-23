package com.racephotos.auth.user;

public record CognitoUserProfile(
    String sub,
    String email,
    String givenName,
    String familyName,
    String pictureUrl
) {
}
