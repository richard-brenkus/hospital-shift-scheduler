package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.config.PasswordEncoderConfig;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserRegisterForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserUpdateForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserViewRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserValidationResult;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ValidationError;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.UserMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoderConfig encoder;
    private final UserMapper userMapper;
    private final UserTransactionalUpdater transactionalUpdater;
    private final UserTransactionalCreator userTransactionalCreator;
    private final ActivityPublisher activityPublisher;

    public String getDisplayNameByUserName(String userName) {
        User currentUser = userRepository.getUserByUsername(userName);
        String displayName = currentUser.getName();
        if (currentUser.getTitle() != null)
            displayName = currentUser.getTitle() + " " + displayName;

        return displayName;
    }

    public User getUserByUsername(String username) {
        return userRepository.getUserByUsername(username);
    }

    public void changeUserPassword(String username, String newPassword) {
        User user = userRepository.getUserByUsername(username);

        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }

        String newEncodedPassword = encoder.passwordEncoder().encode(newPassword);

        user.setPassword(newEncodedPassword);
        userRepository.saveAndFlush(user);

        activityPublisher.publishSuccess(
                ActivityType.PASSWORD_CHANGED,
                "User",
                user.getId().toString(),
                "User password changed"
        );
    }

    public boolean hasShiftRequest(String username) {
        User user = userRepository.getUserByUsername(username);
        return user != null && user.hasShiftRequest();
    }

    @Transactional
    public UserValidationResult validateAndCreateUser(UserRegisterForm form) {

        UserValidationResult validationResult = new UserValidationResult(true, new ArrayList<>(), new ArrayList<>());

        if (form.getAllowedShiftTypes() == null || form.getAllowedShiftTypes().isEmpty()) {
            validationResult.getFieldErrors().add(new ValidationError("allowedShiftTypes", "error.allowedShiftTypes"));
            validationResult.setValid(false);
        }

        if (this.existsByUsernameIgnoreCase(form.getUsername().trim().toLowerCase(Locale.ROOT))) {
            validationResult.getFieldErrors().add(new ValidationError("username", "error.username"));
            validationResult.setValid(false);
        }

        if (this.existsByEmailIgnoreCase(form.getEmail().trim().toLowerCase(Locale.ROOT))) {
            validationResult.getFieldErrors().add(new ValidationError("email", "error.email"));
            validationResult.setValid(false);
        }

        if (this.existsByNameIgnoreCase(form.getName().trim())) {
            validationResult.getFieldErrors().add(new ValidationError("name", "error.name"));
            validationResult.setValid(false);
        }

        if (!validationResult.getFieldErrors().isEmpty()) {
            validationResult.setValid(false);
            return validationResult;
        }

        try {
            userTransactionalCreator.createUser(form);
        } catch (DataIntegrityViolationException exception) {
            validationResult.addGlobalError(new ValidationError("concurrentDuplicate", "error.database"));
            validationResult.setValid(false);
        }

        return validationResult;
    }

    public boolean existsByUsernameIgnoreCase(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    public boolean existsByEmailIgnoreCase(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    public boolean existsByNameIgnoreCase(String name) {
        return userRepository.existsByNameIgnoreCase(name);
    }

    public List<User> getAllUsersWithoutAdminByNameAsc() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isAdmin())
                .sorted(Comparator.comparing(User::getName))
                .toList();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + userId));
    }

    public String getUsernameByUserId(long userId) {
        return userRepository.getUserById(userId).getUsername();
    }

    public void deleteUser(User user) {
        Long userId = user.getId();

        userRepository.delete(user);
        userRepository.flush();

        activityPublisher.publishSuccess(
                ActivityType.USER_DELETED,
                "User",
                userId.toString(),
                "User account deleted"
        );
    }

    public List<User> getAllUsersWithShiftRequestByNameAsc() {
        return userRepository.findAll().stream()
                .filter(User::hasShiftRequest)
                .sorted(Comparator.comparing(User::getName))
                .toList();
    }

    public List<User> getAllUsersAndAdminsByNameAsc() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getName))
                .toList();
    }

    public List<UserViewRecord> getAllUserSummaryViewRecordsByNameAsc() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getName))
                .map(userMapper::entityToUserViewRecord)
                .toList();
    }

    public UserValidationResult validateAndUpdateUser(UserUpdateForm updatedUser) {
        UserValidationResult result = new UserValidationResult(true, new ArrayList<>(), new ArrayList<>());

        Long id = updatedUser.getId();

        if (!userRepository.existsById(id)) {
            result.addGlobalError(new ValidationError("userNotFound", "The selected user no longer exists."));
            result.setValid(false);
            return result;
        }

        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(updatedUser.getUsername().trim().toLowerCase(Locale.ROOT), id)) {
            result.addFieldError(new ValidationError("username", "error.username"));
        }

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(updatedUser.getEmail().trim().toLowerCase(Locale.ROOT), id)) {
            result.addFieldError(new ValidationError("email", "error.email"));
        }

        if (userRepository.existsByNameIgnoreCaseAndIdNot(updatedUser.getName().trim(), id)) {
            result.addFieldError(new ValidationError("name", "error.name"));
        }

        if (updatedUser.getAllowedShiftTypes() == null || updatedUser.getAllowedShiftTypes().isEmpty()) {
            result.addFieldError(new ValidationError("allowedShiftTypes", "error.allowedShiftTypes"));
        }

        if (!result.getFieldErrors().isEmpty() || !result.getGlobalErrors().isEmpty()) {
            result.setValid(false);
            return result;
        }

        try {
            transactionalUpdater.update(updatedUser);
        } catch (ObjectOptimisticLockingFailureException exception) {
            result.addGlobalError(new ValidationError("concurrentUpdate", "error.concurrentUpdate"));
            result.setValid(false);
        } catch (DataIntegrityViolationException exception) {
            result.addGlobalError(new ValidationError("concurrentDuplicate", "error.concurrentDuplicate"));
            result.setValid(false);
        }

        return result;
    }

    @Transactional
    public List<UserViewRecord> findAllUsersForSelectionByNameAsc() {

        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getName))
                .map(userMapper::entityToUserViewRecord)
                .toList();
    }

    public UserViewRecord findUserViewById(Long userId) {

        return userRepository.findById(userId)
                .map(userMapper::entityToUserViewRecord)
                .orElse(null);
    }

    public UserUpdateForm getUserUpdateFormByUserId(long userId) {
        User user = userRepository.getUserById(userId);
        return userMapper.entityToUserUpdateFormByUserId(user);
    }

}
