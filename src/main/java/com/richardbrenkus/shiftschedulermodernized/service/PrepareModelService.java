package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.SelectionLists;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeName;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeValue;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Profession;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
public class PrepareModelService {

    private final ShiftTypeService shiftTypeService;
    private final UserService userService;

    public PrepareModelService(ShiftTypeService shiftTypeService, UserService userService) {
        this.shiftTypeService = shiftTypeService;
        this.userService = userService;
    }

    public void populateUserIndexModelForUser(Model model, ShiftRequestForm shiftRequestForm, boolean isAdmin, String usernamePassed) {
        model.addAttribute(ModelAttributeName.SHIFT_REQUEST_FORM, shiftRequestForm);
        model.addAttribute(ModelAttributeName.WEEKEND_COUNT_LIST, SelectionLists.WEEKEND_COUNT_LIST);
        model.addAttribute(ModelAttributeName.WEEKDAY_COUNT_LIST, SelectionLists.WEEKDAY_COUNT_LIST);
        model.addAttribute(ModelAttributeName.PRIORITY_LIST, SelectionLists.PRIORITY_LIST);
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
        model.addAttribute(ModelAttributeName.HEADER_TYPE, ModelAttributeValue.HEADER_TYPE_ADMIN_ADD);
    }
}

