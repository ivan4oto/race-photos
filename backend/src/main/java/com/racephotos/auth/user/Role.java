package com.racephotos.auth.user;

public enum Role {
    ADMIN,
    BASIC,
    PHOTOGRAPHER,
    SUPPORT;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}
