package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculationInput;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.CalculationProfile;
import com.richardbrenkus.shiftschedulermodernized.algorithm.record.UserCalculationData;
import com.richardbrenkus.shiftschedulermodernized.config.SelectionLists;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CalculationProfileForm;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredUserSnapshot;
import com.richardbrenkus.shiftschedulermodernized.mapper.UserCalculationDataMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.service.ScheduleRuleService;
import com.richardbrenkus.shiftschedulermodernized.service.ShiftTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CalculationInputLoader {

    private final UserRepository userRepository;
    private final ShiftTypeService shiftTypeService;
    private final ScheduleRuleService scheduleRuleService;
    private final UserCalculationDataMapper userCalculationDataMapper;

    @Transactional(readOnly = true)
    public CalculationInput load(CalculationProfileForm form) {
        YearMonth month = form.getCalculationMonth();
        List<Integer> shiftTypes = List.copyOf(shiftTypeService.getShiftTypes());
        List<Integer> calculationOrder = resolveShiftCalculationOrder(form, shiftTypes);
        List<Integer> priorities = SelectionLists.GENERIC_ONE_TO_TEN_LIST;
        List<LocalDate> holidays = getHolidaysCzechRepublic(month);

        Map<Integer, StoredScheduleDay> previousStoredDays = scheduleRuleService.loadPreviousStoredScheduleDays(month.atDay(1), form.getGapBetweenShifts());
        Map<Long, Set<LocalDate>> previousDatesByUser = mapPreviousAssignmentsByUser(month, previousStoredDays);

        List<UserCalculationData> users = userRepository.findAllByEnabledTrueAndShiftRequestIsNotNullOrderByNameAsc().stream().map(user -> userCalculationDataMapper.toCalculationData(user, previousDatesByUser.getOrDefault(user.getId(), Set.of()))).toList();

        CalculationProfile profile = new CalculationProfile(form.getShiftCountCap(), form.getGapBetweenShifts(), form.isSortByDatesAmount(), form.getForceFillShiftTypes());

        return new CalculationInput(month, users, shiftTypes, calculationOrder, priorities, holidays, profile);
    }

    private List<Integer> resolveShiftCalculationOrder(CalculationProfileForm form, List<Integer> shiftTypes) {

        List<Integer> forceFill = form.getForceFillShiftTypes() == null ? List.of() : form.getForceFillShiftTypes();
        List<Integer> result = new ArrayList<>(forceFill);
        shiftTypes.stream().filter(type -> !forceFill.contains(type)).forEach(result::add);

        return List.copyOf(result);
    }

    private Map<Long, Set<LocalDate>> mapPreviousAssignmentsByUser(YearMonth calculationMonth, Map<Integer, StoredScheduleDay> previousStoredDays) {

        Map<Long, Set<LocalDate>> result = new HashMap<>();
        LocalDate firstDay = calculationMonth.atDay(1);

        previousStoredDays.forEach((backwardIndex, storedDay) -> {
            if (storedDay == null || storedDay.getAssignmentsByShiftType() == null) return;

            // key 0 = previous month's final day, key -1 = one day earlier, etc.
            LocalDate assignmentDate = firstDay.minusDays(1L - backwardIndex);

            for (StoredUserSnapshot snapshot : storedDay.getAssignmentsByShiftType().values()) {
                if (snapshot == null || snapshot.getUserId() == null) continue;
                result.computeIfAbsent(snapshot.getUserId(), ignored -> new HashSet<>()).add(assignmentDate);
            }
        });

        result.replaceAll((id, dates) -> Set.copyOf(dates));

        return Map.copyOf(result);
    }

    private List<LocalDate> getHolidaysCzechRepublic(YearMonth month) {

        int year = month.getYear();
        LocalDate goodFriday = calculateGoodFriday(year);

        return List.of(LocalDate.of(year, 1, 1), goodFriday, goodFriday.plusDays(3), LocalDate.of(year, 5, 1), LocalDate.of(year, 5, 8), LocalDate.of(year, 7, 5), LocalDate.of(year, 7, 6), LocalDate.of(year, 9, 28), LocalDate.of(year, 10, 28), LocalDate.of(year, 11, 17), LocalDate.of(year, 12, 24), LocalDate.of(year, 12, 25), LocalDate.of(year, 12, 26));
    }

    private static LocalDate calculateGoodFriday(int year) {
        int g = year % 19;
        int c = year / 100;
        int h = (c - (c / 4) - ((8 * c + 13) / 25) + 19 * g + 15) % 30;
        int i = h - (h / 28) * (1 - (h / 28) * (29 / (h + 1)) * ((21 - g) / 11));
        int day = (i - ((year + (year / 4) + i + 2 - c + (c / 4)) % 7) + 28) - 2;
        int month = 3;
        if (day > 31) {
            month++;
            day -= 31;
        }
        return LocalDate.of(year, month, day);
    }
}