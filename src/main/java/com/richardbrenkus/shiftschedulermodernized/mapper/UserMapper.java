package com.richardbrenkus.shiftschedulermodernized.mapper;

import com.richardbrenkus.shiftschedulermodernized.dto.form.UserUpdateForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final UserRepository userRepository;

    public UserMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
        userRegisterForm.setAllowedShiftTypes(user.getAllowedShiftTypes());
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

    public UserUpdateForm entityToUserUpdateFormByUserId(long userId) {
        User user = userRepository.getUserById(userId);
        return entityToUserUpdateForm(user);
    }
}
