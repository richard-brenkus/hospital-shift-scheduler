package com.richardbrenkus.shiftschedulermodernized.mapper;

import com.richardbrenkus.shiftschedulermodernized.dto.form.UserRegisterForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserUpdateForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    UserRepository userRepository;

    public UserMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserUpdateForm entityToUserUpdateForm(User user) {
        UserUpdateForm userRegisterForm = new UserUpdateForm();
        userRegisterForm.setId(user.getId());
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

    public User userRegisterFormToEntity(UserRegisterForm userRegisterForm) {
        User user = new User();
        user.setId(userRegisterForm.getId());
        user.setTitle(userRegisterForm.getTitle());
        user.setName(userRegisterForm.getName());
        user.setUsername(userRegisterForm.getUsername());
        user.setPassword(userRegisterForm.getPassword());
        user.setNote(userRegisterForm.getNote());
        user.setEmail(userRegisterForm.getEmail());
        user.setBirthday(userRegisterForm.getBirthday());
        user.setProfession(userRegisterForm.getProfession());
        user.setAllowedShiftTypes(userRegisterForm.getAllowedShiftTypes());
        return user;
    }

    public UserViewRecord entityToUserViewRecord(User user) {
        return UserViewRecord.builder()
                .userId(user.getId())
                .name(user.getName())
                .username(user.getUsername())   // missing
                .email(user.getEmail())
                .hasShiftRequest(user.hasShiftRequest())
                .build();
    }

    public UserUpdateForm entityToUserUpdateFormByUserId(long userId) {
        User user = userRepository.getUserById(userId);
        return entityToUserUpdateForm(user);
    }
}
