package com.richardbrenkus.shiftschedulermodernized.dto.view;

import java.util.List;

public record UserUpdateValidationResult(
        boolean isValid,
        List<String> defaultMessages,
        List<String> rejectedFields
) {
    public static UserUpdateValidationResult valid() {
        return new UserUpdateValidationResult(true, List.of(), List.of());
    }
}
