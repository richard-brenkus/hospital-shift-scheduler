package com.richardbrenkus.shiftschedulermodernized.mapper;

import com.richardbrenkus.shiftschedulermodernized.dto.form.UserForm;
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


    public UserForm entityToUserForm(User user) {
        UserForm userForm = new UserForm();
        userForm.setId(user.getId());
        userForm.setTitle(user.getTitle());
        userForm.setName(user.getName());
        userForm.setUsername(user.getUsername());
        userForm.setPassword(user.getPassword());
        userForm.setNote(user.getNote());
        userForm.setEmail(user.getEmail());
        userForm.setBirthday(user.getBirthday());
        userForm.setProfession(user.getProfession());
        userForm.setAllowedShiftTypes(user.getAllowedShiftTypes());
        return userForm;
    }

    public User userFormToEntity(UserForm userForm) {
        User user = new User();
        user.setId(userForm.getId());
        user.setTitle(userForm.getTitle());
        user.setName(userForm.getName());
        user.setUsername(userForm.getUsername());
        user.setPassword(userForm.getPassword());
        user.setNote(userForm.getNote());
        user.setEmail(userForm.getEmail());
        user.setBirthday(userForm.getBirthday());
        user.setProfession(userForm.getProfession());
        user.setAllowedShiftTypes(userForm.getAllowedShiftTypes());
        return user;
    }

    public UserViewRecord entityToUserViewRecord(User user) {
        return UserViewRecord.builder()
                .userId(user.getId())
                .name(user.getName())
                .username(user.getUsername())   // missing
                .email(user.getEmail())
                .build();
    }

    public UserForm entityToUserFormByUserId(long userId) {
        User user = userRepository.getUserById(userId);
        return entityToUserForm(user);
    }
}
