package com.richardbrenkus.shiftschedulermodernized.dto.view;

import java.time.YearMonth;

public record MonthOption(
        YearMonth value,
        String label
) {
}
