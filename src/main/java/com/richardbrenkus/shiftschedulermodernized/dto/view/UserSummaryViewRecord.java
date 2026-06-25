package com.richardbrenkus.shiftschedulermodernized.dto.view;

import lombok.Builder;

@Builder
public record UserSummaryViewRecord(
        Long userId,
        String name,
        String username,
        String email,
        boolean hasShiftRequest
){}
