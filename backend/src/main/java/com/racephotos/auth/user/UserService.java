package com.racephotos.auth.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    }

    @Transactional
    public User syncFromCognito(CognitoUserProfile cognitoUser) {
        if (cognitoUser == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cognito user profile is required");
        }

        String sub = normalize(cognitoUser.sub());
        String email = normalizeEmail(cognitoUser.email());
        if (sub == null || email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cognito token missing required claims");
        }

        User user = userRepository.findByCognitoSub(sub).orElseGet(() -> {
            User created = new User();
            created.setCognitoSub(sub);
            created.setRoles(EnumSet.of(Role.BASIC));
            return created;
        });

        boolean dirty = user.getId() == null;
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(EnumSet.of(Role.BASIC));
            dirty = true;
        }
        dirty |= setIfChanged(user::getEmail, user::setEmail, email);
        dirty |= setIfChanged(user::getFirstName, user::setFirstName, normalize(cognitoUser.givenName()));
        dirty |= setIfChanged(user::getFamilyName, user::setFamilyName, normalize(cognitoUser.familyName()));
        dirty |= setIfChanged(user::getProfilePictureUrl, user::setProfilePictureUrl, normalize(cognitoUser.pictureUrl()));

        return dirty ? userRepository.save(user) : user;
    }

    private boolean setIfChanged(Supplier<String> getter, Consumer<String> setter, String value) {
        String current = getter.get();
        if (!Objects.equals(current, value)) {
            setter.accept(value);
            return true;
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(String email) {
        String normalized = normalize(email);
        return normalized == null ? null : normalized.toLowerCase();
    }
}
