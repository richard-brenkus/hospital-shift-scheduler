package com.richardbrenkus.shiftschedulermodernized.dto.form;

import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDayForm {

    private LocalDate date;
    private boolean weekendOrHoliday;

    @Builder.Default
    private List<ShiftAssignmentForm> assignments = new ArrayList<>();
}
