package com.richardbrenkus.hospitalshiftscheduler.config.constants;

public enum Role {
    USER,
    ADMIN,
    SYSTEM,
    UNKNOWN,
    DEMO_ADMIN;

    public String asAuthority() {
        return "ROLE_" + this.name();
    }
}
