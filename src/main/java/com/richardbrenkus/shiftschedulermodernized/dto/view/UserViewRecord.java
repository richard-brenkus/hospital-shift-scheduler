package com.richardbrenkus.shiftschedulermodernized.dto.view;

import lombok.Builder;

@Builder
public record UserViewRecord(
        Long userId,
        String name,
        String username,
        String email
){}
