package com.richardbrenkus.shiftschedulermodernized.config;

import java.util.List;
import java.util.stream.IntStream;

public final class SelectionLists {

    private SelectionLists() {
    }

    public static final List<Integer> WEEKEND_COUNT_LIST = IntStream.rangeClosed(1, 8).boxed().toList();

    public static final List<Integer> GENERIC_ONE_TO_TEN_LIST = IntStream.rangeClosed(1, 10).boxed().toList();

}
