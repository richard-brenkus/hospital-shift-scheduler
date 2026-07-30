package com.richardbrenkus.hospitalshiftscheduler.dto.view;

import java.util.List;

public record ShiftRequestValidationResult(boolean isValid,
                                           String modelFlag,
                                           List<String> rejectedFields
) {
    public static ShiftRequestValidationResult valid() {
        return new ShiftRequestValidationResult(true, null, List.of());
    }

    public static ShiftRequestValidationResult invalid(String modelFlag, String... rejectedFields) {
        return new ShiftRequestValidationResult(false, modelFlag, List.of(rejectedFields));
    }
}
