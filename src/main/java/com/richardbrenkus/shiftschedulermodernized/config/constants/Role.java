package com.richardbrenkus.shiftschedulermodernized.config.constants;

public enum Role {
    USER,
    ADMIN;

    public String asAuthority() {
        return "ROLE_" + this.name();
    }
}
