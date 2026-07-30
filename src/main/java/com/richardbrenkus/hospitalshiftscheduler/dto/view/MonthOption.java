package com.richardbrenkus.hospitalshiftscheduler.dto.view;

import java.time.YearMonth;

public record MonthOption(
        YearMonth value,
        String label
) {
}
