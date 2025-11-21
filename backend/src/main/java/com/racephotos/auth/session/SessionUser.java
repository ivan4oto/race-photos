package com.racephotos.auth.session;

public record SessionUser(
    String sub,
    String email,
    String givenName,
    String familyName
) {
}
