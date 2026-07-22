package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleEditForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ScheduleMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleMapperService {

    private final UserRepository userRepository;
    private final ScheduleMapper scheduleMapper;

    @Transactional
    public ScheduleMonth toScheduleMonth(ScheduleEditForm scheduleEditForm) {
        Map<Long, User> usersById = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return scheduleMapper.toScheduleMonth(scheduleEditForm, scheduleEditForm.toCalculationProfileForm(), usersById);

    }
}
