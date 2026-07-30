package com.richardbrenkus.hospitalshiftscheduler.algorithm;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class CalculationCounters {

    private final Map<Long, Map<Integer, Integer>> weekdayCounters = new HashMap<>();
    private final Map<Long, Map<Integer, Integer>> weekendCounters = new HashMap<>();

    public void incrementWeekday(Long userId, int shiftType) {
        weekdayCounters.computeIfAbsent(userId, ignored -> new HashMap<>()).merge(shiftType, 1, Integer::sum);
    }

    public void incrementWeekend(Long userId, int shiftType) {
        weekendCounters.computeIfAbsent(userId, ignored -> new HashMap<>()).merge(shiftType, 1, Integer::sum);
    }

    public int getWeekdayCount(Long userId, int shiftType) {
        return weekdayCounters.getOrDefault(userId, Map.of()).getOrDefault(shiftType, 0);
    }

    public int getWeekendCount(Long userId, int shiftType) {
        return weekendCounters.getOrDefault(userId, Map.of()).getOrDefault(shiftType, 0);
    }

    public int getTotalCount(Long userId) {
        int weekdayTotal = weekdayCounters.getOrDefault(userId, Map.of()).values().stream().mapToInt(Integer::intValue).sum();
        int weekendTotal = weekendCounters.getOrDefault(userId, Map.of()).values().stream().mapToInt(Integer::intValue).sum();

        return weekdayTotal + weekendTotal;
    }

}
