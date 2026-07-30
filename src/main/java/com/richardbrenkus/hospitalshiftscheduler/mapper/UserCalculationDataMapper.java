package com.richardbrenkus.hospitalshiftscheduler.mapper;

import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.ShiftPreferenceCalculationData;
import com.richardbrenkus.hospitalshiftscheduler.algorithm.record.UserCalculationData;
import com.richardbrenkus.hospitalshiftscheduler.entity.ShiftPreference;
import com.richardbrenkus.hospitalshiftscheduler.entity.ShiftRequest;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class UserCalculationDataMapper {

    public UserCalculationData toCalculationData(User user, Set<LocalDate> previousMonthAssignedDates) {
        ShiftRequest request = user.getShiftRequest();

        Set<Integer> allowedShiftTypes = user.getAllowedShiftTypes() == null ? Set.of() : Set.copyOf(user.getAllowedShiftTypes());

        Set<LocalDate> unavailableDates = request == null || request.getDatesNo() == null ? Set.of() : Set.copyOf(request.getDatesNo());

        List<ShiftPreferenceCalculationData> preferences = request == null ? List.of() : request.getPreferences().stream().map(this::toPreferenceData).toList();

        Map<Integer, ShiftPreferenceCalculationData> preferencesByShiftType = UserCalculationData.indexPreferences(preferences);

        return new UserCalculationData(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getTitle(),
                allowedShiftTypes,
                unavailableDates,
                preferencesByShiftType, previousMonthAssignedDates == null ? Set.of() : Set.copyOf(previousMonthAssignedDates),
                user.hasShiftRequest()
        );
    }

    private ShiftPreferenceCalculationData toPreferenceData(ShiftPreference preference) {
        Set<LocalDate> datesYes = preference.getDatesYes() == null ? Set.of() : Set.copyOf(preference.getDatesYes());

        return new ShiftPreferenceCalculationData(
                preference.getShiftType(),
                preference.getPriority(),
                preference.getWeekdayCount(),
                preference.getWeekendCount(),
                preference.isNoShiftRequested(),
                preference.isAnyDateSelected(),
                datesYes
        );
    }
}
