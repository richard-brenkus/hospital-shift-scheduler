package com.richardbrenkus.shiftschedulermodernized.dto.form;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CleanupTaskForm {

    private boolean isCleanupTaskActive;
    private LocalDateTime cleanupExecutionTime;
    private int cleanupDay;
    private int cleanupHour;
    private int cleanupMinute;

}
