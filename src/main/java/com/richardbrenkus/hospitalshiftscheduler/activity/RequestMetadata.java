package com.richardbrenkus.hospitalshiftscheduler.activity;

public record RequestMetadata(String requestMethod, String requestPath, String clientIp) {

    private static final String SYSTEM_VALUE = "SYSTEM";

    public static RequestMetadata system() {
        return new RequestMetadata(SYSTEM_VALUE, SYSTEM_VALUE, SYSTEM_VALUE);
    }
}