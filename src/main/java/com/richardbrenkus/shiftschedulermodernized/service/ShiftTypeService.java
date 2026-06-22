package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.ShiftTypeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ShiftTypeService {

    private final ShiftTypeProperties shiftTypeProperties;

    public List<Integer> getShiftTypes() {
        return IntStream.rangeClosed(1, shiftTypeProperties.count())
                .boxed()
                .toList();
    }
}
