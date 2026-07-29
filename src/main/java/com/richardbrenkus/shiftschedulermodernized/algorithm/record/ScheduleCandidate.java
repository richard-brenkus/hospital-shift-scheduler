package com.richardbrenkus.shiftschedulermodernized.algorithm.record;

import java.util.Objects;

public record ScheduleCandidate(CalculatedScheduleMonth scheduleMonth, int hitCounter, int workerIndex, int attemptIndex, long randomSeed) {

    public ScheduleCandidate {
        Objects.requireNonNull(scheduleMonth, "scheduleMonth must not be null");
        if (hitCounter < 0) throw new IllegalArgumentException("hitCounter must not be negative");
        if (workerIndex < 0) throw new IllegalArgumentException("workerIndex must not be negative");
        if (attemptIndex < 0) throw new IllegalArgumentException("attemptIndex must not be negative");
        if (scheduleMonth.getHitCounter() != hitCounter) {
            throw new IllegalArgumentException("Candidate hitCounter does not match schedule hitCounter");
        }
    }

    public static ScheduleCandidate from(CalculatedScheduleMonth scheduleMonth, int workerIndex, int attemptIndex, long randomSeed) {
        return new ScheduleCandidate(scheduleMonth, scheduleMonth.getHitCounter(), workerIndex, attemptIndex, randomSeed);
    }
}
