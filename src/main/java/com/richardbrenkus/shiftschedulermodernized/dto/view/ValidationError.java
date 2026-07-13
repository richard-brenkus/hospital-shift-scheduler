package com.richardbrenkus.shiftschedulermodernized.dto.view;

public record ValidationError(
        String field,
        String message
) {}
