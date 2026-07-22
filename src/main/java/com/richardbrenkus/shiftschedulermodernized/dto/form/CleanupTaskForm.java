package com.richardbrenkus.shiftschedulermodernized.dto.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CleanupTaskForm {

    private boolean isCleanupTaskActive;

    @Min(1)
    @Max(31)
    private int cleanupDay;

    @Min(0)
    @Max(23)
    private int cleanupHour;

    @Min(0)
    @Max(59)
    private int cleanupMinute;

}
