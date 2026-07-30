package com.richardbrenkus.hospitalshiftscheduler.service;

import com.richardbrenkus.hospitalshiftscheduler.config.SelectionLists;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ModelAttributeName;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.ModelAttributeValue;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.Profession;
import com.richardbrenkus.hospitalshiftscheduler.dto.form.ShiftRequestForm;
import com.richardbrenkus.hospitalshiftscheduler.entity.ShiftRequest;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import com.richardbrenkus.hospitalshiftscheduler.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrepareModelServiceTest {

    @Mock
    private ShiftTypeService shiftTypeService;

    @Mock
    private UserService userService;

    @Mock
    private Model model;

    @InjectMocks
    private PrepareModelService service;

    @Test
    void shouldPopulateAllAttributes_whenPreparingUserIndexForUser() {
        ShiftRequestForm form = new ShiftRequestForm();

        service.populateUserIndexModelForUser(model, form, false, "freddie");

        verify(model).addAttribute(ModelAttributeName.SHIFT_REQUEST_FORM, form);
        verify(model).addAttribute(ModelAttributeName.WEEKEND_COUNT_LIST, SelectionLists.WEEKEND_COUNT_LIST);
        verify(model).addAttribute(ModelAttributeName.WEEKDAY_COUNT_LIST, SelectionLists.GENERIC_ONE_TO_TEN_LIST);
        verify(model).addAttribute(ModelAttributeName.PRIORITY_LIST, SelectionLists.GENERIC_ONE_TO_TEN_LIST);
        verify(model).addAttribute(ModelAttributeName.IS_ADMIN, false);
        verify(model).addAttribute(ModelAttributeName.USERNAME_PASSED, "freddie");
    }

    @Test
    void shouldAddAdminSpecificAttributes_whenPreparingUserIndexForAdmin() {
        ShiftRequestForm form = new ShiftRequestForm();
        User user = TestFixtures.user(42L, "mick");
        user.setShiftRequest(new ShiftRequest());
        when(userService.getUserById(42L)).thenReturn(user);
        when(userService.getDisplayNameByUserName("mick")).thenReturn("Mick Jagger");

        service.populateUserIndexModelForAdmin(model, form, true, 42L);

        verify(model).addAttribute(ModelAttributeName.SHIFT_REQUEST_FORM, form);
        verify(model).addAttribute(ModelAttributeName.IS_ADMIN, true);
        verify(model).addAttribute(ModelAttributeName.USERNAME_PASSED, "mick");
        verify(model).addAttribute(ModelAttributeName.CURRENT_PRIORITY, 5);
        verify(model).addAttribute(ModelAttributeName.DISPLAY_NAME, "Mick Jagger");
        verify(model).addAttribute(ModelAttributeName.USERNAME, "mick");
        verify(model).addAttribute(ModelAttributeName.HAS_SHIFT_REQUEST, Boolean.TRUE);
    }

    @Test
    void shouldPopulateAdminModelWithoutShiftRequest_whenUserHasNoRequest() {
        ShiftRequestForm form = new ShiftRequestForm();
        User user = TestFixtures.user(42L, "mick");
        when(userService.getUserById(42L)).thenReturn(user);
        when(userService.getDisplayNameByUserName("mick")).thenReturn("Mick");

        service.populateUserIndexModelForAdmin(model, form, true, 42L);

        verify(model).addAttribute(ModelAttributeName.HAS_SHIFT_REQUEST, Boolean.FALSE);
    }

    @Test
    void shouldAddProfessionsAndShiftTypes_whenPreparingRegisterUserModel() {
        List<Integer> shiftTypes = List.of(1, 2, 3);
        when(shiftTypeService.getShiftTypes()).thenReturn(shiftTypes);

        service.prepareRegisterUserModel(model);

        verify(model).addAttribute(ModelAttributeName.PROFESSIONS, Profession.values());
        verify(model).addAttribute(ModelAttributeName.SHIFT_TYPES, shiftTypes);
        verify(model).addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_ADD);
    }

    @Test
    void shouldSetActionTypeUpdate_whenPreparingUpdateUserModel() {
        List<Integer> shiftTypes = List.of(1, 2);
        when(shiftTypeService.getShiftTypes()).thenReturn(shiftTypes);

        service.prepareUpdateUserModel(model);

        verify(model).addAttribute(ModelAttributeName.PROFESSIONS, Profession.values());
        verify(model).addAttribute(ModelAttributeName.SHIFT_TYPES, shiftTypes);
        verify(model).addAttribute(ModelAttributeName.ACTION_TYPE, ModelAttributeValue.ACTION_TYPE_UPDATE);
    }
}
