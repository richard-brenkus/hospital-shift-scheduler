package com.richardbrenkus.hospitalshiftscheduler.dto.view;

public record ValidationError(
        String field,
        String message
) {}
