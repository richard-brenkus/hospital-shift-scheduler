package com.richardbrenkus.hospitalshiftscheduler.dto.form;

import lombok.*;

import java.time.YearMonth;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedScheduleSelectionForm {

    private YearMonth selectedMonth;
}

