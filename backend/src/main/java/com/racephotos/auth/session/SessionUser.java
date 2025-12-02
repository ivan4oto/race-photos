package com.racephotos.auth.session;

import com.racephotos.auth.user.User;

import com.racephotos.auth.user.Role;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record SessionUser(
    UUID id,
    String sub,
    String email,
    String givenName,
    String familyName,
    String profilePictureUrl,
    Set<Role> roles,
    Set<UUID> accessibleEventIds
) implements Serializable {
    public static SessionUser from(User user) {
        if (user == null) {
            return null;
        }
        Set<Role> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = Set.of(Role.BASIC);
        }
        return new SessionUser(
            user.getId(),
            user.getCognitoSub(),
            user.getEmail(),
            user.getFirstName(),
            user.getFamilyName(),
            user.getProfilePictureUrl(),
            Collections.unmodifiableSet(roles),
            Collections.unmodifiableSet(user.getActiveEventAccessIds())
        );
    }
}
