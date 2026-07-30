package com.richardbrenkus.hospitalshiftscheduler.mapper;

import com.richardbrenkus.hospitalshiftscheduler.dto.form.UserUpdateForm;
import com.richardbrenkus.hospitalshiftscheduler.dto.view.UserViewRecord;
import com.richardbrenkus.hospitalshiftscheduler.entity.User;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class UserMapper {

    public UserUpdateForm entityToUserUpdateForm(User user) {
        UserUpdateForm userRegisterForm = new UserUpdateForm();
        userRegisterForm.setId(user.getId());
        userRegisterForm.setVersion(user.getVersion());
        userRegisterForm.setTitle(user.getTitle());
        userRegisterForm.setName(user.getName());
        userRegisterForm.setUsername(user.getUsername());
        userRegisterForm.setNote(user.getNote());
        userRegisterForm.setEmail(user.getEmail());
        userRegisterForm.setBirthday(user.getBirthday());
        userRegisterForm.setProfession(user.getProfession());
        userRegisterForm.setAllowedShiftTypes(new HashSet<>(user.getAllowedShiftTypes()));
        return userRegisterForm;
    }

    public UserViewRecord entityToUserViewRecord(User user) {
        return UserViewRecord.builder()
                .userId(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .hasShiftRequest(user.hasShiftRequest())
                .build();
    }

    public UserUpdateForm entityToUserUpdateFormByUserId(User user) {
        return entityToUserUpdateForm(user);
    }
}
