package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.SelectionLists;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ModelAttributeName;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
public class UserIndexPageService {

    public void populateUserIndexModel(Model model, ShiftRequestForm shiftRequestForm, boolean isAdmin, String usernamePassed) {
        model.addAttribute(ModelAttributeName.SHIFT_REQUEST_FORM, shiftRequestForm);
        model.addAttribute(ModelAttributeName.WEEKEND_COUNT_LIST, SelectionLists.WEEKEND_COUNT_LIST);
        model.addAttribute(ModelAttributeName.WEEKDAY_COUNT_LIST, SelectionLists.WEEKDAY_COUNT_LIST);
        model.addAttribute(ModelAttributeName.PRIORITY_LIST, SelectionLists.PRIORITY_LIST);
        model.addAttribute(ModelAttributeName.IS_ADMIN, isAdmin);
        model.addAttribute(ModelAttributeName.USERNAME_PASSED, usernamePassed);
    }
}

