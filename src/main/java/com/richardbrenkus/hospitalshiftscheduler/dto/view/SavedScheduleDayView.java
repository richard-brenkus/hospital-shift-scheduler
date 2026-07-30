package com.richardbrenkus.hospitalshiftscheduler.dto.view;

import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedScheduleDayView {

    private LocalDate date;
    private boolean weekendOrHoliday;

    @Builder.Default
    private List<SavedScheduleShiftAssignmentView> assignments = new ArrayList<>();
}
