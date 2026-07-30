package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.config.SelectionLists;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ApplicationConstants;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ModelAttributeName;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ModelAttributeValue;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.Profession;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.CalculationProfileForm;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.ShiftRequestForm;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.YearMonth;
import java.util.ArrayList;

@Service
@AllArgsConstructor
public class PrepareModelService {

    private final ShiftTypeService shiftTypeService;
    private final UserService userService;
    private final CalculationProfileService calculationProfileService;

    public void populateUserIndexModelForUser(Model model, ShiftRequestForm shiftRequestForm, boolean isAdmin, String usernamePassed) {
        model.addAttribute(ModelAttributeName.SHIFT_REQUEST_FORM, shiftRequestForm);
        model.addAttribute(ModelAttributeName.WEEKEND_COUNT_LIST, SelectionLists.WEEKEND_COUNT_LIST);
        model.addAttribute(ModelAttributeName.WEEKDAY_COUNT_LIST, SelectionLists.GENERIC_ONE_TO_TEN_LIST);
        model.addAttribute(ModelAttributeName.PRIORITY_LIST, SelectionLists.GENERIC_ONE_TO_TEN_LIST);
        model.addAttribute(ModelAttributeName.IS_ADMIN, isAdmin);
        model.addAttribute(ModelAttributeName.USERNAME_PASSED, usernamePassed);
    }

    public void populateUserIndexModelForAdmin(Model model, ShiftRequestForm shiftRequestForm, boolean isAdmin, long userId) {

        User user = userService.getUserById(userId);
        String username = user.getUsername();
        Boolean hasShiftRequest = user.hasShiftRequest();

        populateUserIndexModelForUser(model, shiftRequestForm, isAdmin, username);

        model.addAttribute(ModelAttributeName.CURRENT_PRIORITY, 5);
        model.addAttribute(ModelAttributeName.DISPLAY_NAME, userService.getDisplayNameByUserName(username));
        model.addAttribute(ModelAttributeName.USERNAME, username);
        model.addAttribute(ModelAttributeName.HAS_SHIFT_REQUEST, hasShiftRequest);
    }

    public void prepareRegisterUserModel(Model model) {
        model.addAttribute(ModelAttributeName.PROFESSIONS, Profession.values());
        model.addAttribute(ModelAttributeName.SHIFT_TYPES, shiftTypeService.getShiftTypes());
        model.addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_ADD);

    }

    public void prepareUpdateUserModel(Model model) {
        model.addAttribute(ModelAttributeName.PROFESSIONS, Profession.values());
        model.addAttribute(ModelAttributeName.SHIFT_TYPES, shiftTypeService.getShiftTypes());
        model.addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_UPDATE);
    }

    public void prepareCalculateScheduleModel(Model model) {
        CalculationProfileForm calculationProfileForm = CalculationProfileForm.builder().calculationMonth(YearMonth.now(ApplicationConstants.ZONE_ID).plusMonths(2)).shiftCountCap(5).gapBetweenShifts(5).forceFillShiftTypes(new ArrayList<>()).build();

        model.addAttribute(ModelAttributeName.CALCULATION_PROFILE_FORM, calculationProfileForm);
        model.addAttribute(ModelAttributeName.MONTH_OPTIONS, calculationProfileService.getAvailableCalculationMonths());
        model.addAttribute(ModelAttributeName.GAP_BETWEEN_SHIFTS, calculationProfileService.getGenericOneToTenList());
        model.addAttribute(ModelAttributeName.SHIFT_COUNT_MAX, calculationProfileService.getGenericOneToTenList());
        model.addAttribute(ModelAttributeName.SHIFT_TYPES, calculationProfileService.getAvailableShiftTypes());
    }
}

