package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleValidationResult;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleEditForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserStatViewRecord;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserStatisticService {

    private static final String SESSION_FULL_USER_STATS = "fullUserStatsByShiftType";
    private static final String SESSION_FULL_STATS_MONTH = "fullStatsMonth";

    public void storeFullStatisticsInSession(
            HttpSession session,
            ScheduleValidationResult validationResult,
            ScheduleEditForm scheduleEditForm
    ) {
        if (session == null) {
            return;
        }

        if (validationResult == null) {
            clearFullStatistics(session);
            return;
        }

        Map<Integer, Set<UserStatViewRecord>> fullStats = validationResult.getFullUserStatsByShiftType() == null
                ? Map.of()
                : validationResult.getFullUserStatsByShiftType();

        session.setAttribute(SESSION_FULL_USER_STATS, fullStats);

        if (scheduleEditForm != null && scheduleEditForm.getMonth() != null) {
            session.setAttribute(SESSION_FULL_STATS_MONTH, scheduleEditForm.getMonth());
        }
    }

    public void addFullStatisticsToModel(Model model, HttpSession session, List<Integer> shiftTypes) {
        Map<Integer, Set<UserStatViewRecord>> fullUserStatsByShiftType =
                getFullUserStatsFromSession(session);

        YearMonth month = getFullStatsMonthFromSession(session);

        boolean statsExist = fullUserStatsByShiftType.values()
                .stream()
                .anyMatch(stats -> stats != null && !stats.isEmpty());

        model.addAttribute("fullUserStatsByShiftType", fullUserStatsByShiftType);

        model.addAttribute("shiftTypes", shiftTypes);
        model.addAttribute("statsExist", statsExist);

        if (month != null) {
            model.addAttribute("month", month);
            model.addAttribute("year", month.getYear());
            model.addAttribute("monthInt", month.getMonthValue());
        }
    }

    public void clearFullStatistics(HttpSession session) {
        if (session == null) {
            return;
        }

        session.removeAttribute(SESSION_FULL_USER_STATS);
        session.removeAttribute(SESSION_FULL_STATS_MONTH);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Set<UserStatViewRecord>> getFullUserStatsFromSession(HttpSession session) {
        if (session == null) {
            return Map.of();
        }

        Object value = session.getAttribute(SESSION_FULL_USER_STATS);

        if (!(value instanceof Map<?, ?>)) {
            return Map.of();
        }

        return (Map<Integer, Set<UserStatViewRecord>>) value;
    }

    private YearMonth getFullStatsMonthFromSession(HttpSession session) {
        if (session == null) {
            return null;
        }

        Object value = session.getAttribute(SESSION_FULL_STATS_MONTH);

        if (value instanceof YearMonth yearMonth) {
            return yearMonth;
        }

        return null;
    }
}
