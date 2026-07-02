package com.richardbrenkus.shiftschedulermodernized.dto.form;

import lombok.*;

import java.time.YearMonth;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedCalendarSelectionForm {

    private YearMonth selectedMonth;
}

