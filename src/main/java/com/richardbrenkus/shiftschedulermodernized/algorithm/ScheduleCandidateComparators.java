package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.algorithm.record.ScheduleCandidate;

import java.util.Comparator;

public final class ScheduleCandidateComparators {

    private ScheduleCandidateComparators() {
    }

    /**
     * Selects the candidate with the highest hit counter.
     * If two candidates have identical hit counters,
     * the lower worker index wins and then the lower
     * attempt index wins.
     * This makes winner selection deterministic and
     * reproducible across runs.
     */
    public static final Comparator<ScheduleCandidate> BY_QUALITY =
            Comparator.comparingInt(ScheduleCandidate::hitCounter)
                    .thenComparingInt(candidate -> -candidate.workerIndex())
                    .thenComparingInt(candidate -> -candidate.attemptIndex());

    /**
     * Same semantics as BY_QUALITY but expressed in the
     * natural ordering direction so it can be used with min().
     */
    public static final Comparator<ScheduleCandidate> BY_QUALITY_ASC =
            Comparator.comparingInt(ScheduleCandidate::hitCounter)
                    .thenComparingInt(ScheduleCandidate::workerIndex)
                    .thenComparingInt(ScheduleCandidate::attemptIndex);
}
