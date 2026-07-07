package com.richardbrenkus.shiftschedulermodernized.dto.export;

public record UserExportRecord(
        Long userId,
        String email,
        String name,
        String username,
        String role,
        boolean enabled
) {

}
