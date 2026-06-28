package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.entity.User;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class CalculationCounters {

    private final Map<Long, Map<Integer, Integer>> weekdayCounters = new HashMap<>();
    private final Map<Long, Map<Integer, Integer>> weekendCounters = new HashMap<>();

    public void incrementWeekday(User user, int shiftType) {
        weekdayCounters
                .computeIfAbsent(user.getId(), id -> new HashMap<>())
                .merge(shiftType, 1, Integer::sum);
    }

    public void incrementWeekend(User user, int shiftType) {
        weekendCounters
                .computeIfAbsent(user.getId(), id -> new HashMap<>())
                .merge(shiftType, 1, Integer::sum);
    }

    public int getWeekdayCount(User user, int shiftType) {
        return weekdayCounters
                .getOrDefault(user.getId(), Map.of())
                .getOrDefault(shiftType, 0);
    }

    public int getWeekendCount(User user, int shiftType) {
        return weekendCounters
                .getOrDefault(user.getId(), Map.of())
                .getOrDefault(shiftType, 0);
    }

    public int getTotalCount(User user) {
        int weekdayTotal = weekdayCounters
                .getOrDefault(user.getId(), Map.of())
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();

        int weekendTotal = weekendCounters
                .getOrDefault(user.getId(), Map.of())
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();

        return weekdayTotal + weekendTotal;
    }
}
