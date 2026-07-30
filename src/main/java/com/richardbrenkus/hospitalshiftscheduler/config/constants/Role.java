package com.richardbrenkus.hospitalshiftscheduler.config.constants;

public enum Role {
    USER,
    ADMIN,
    SYSTEM,
    UNKNOWN;

    public String asAuthority() {
        return "ROLE_" + this.name();
    }
}
