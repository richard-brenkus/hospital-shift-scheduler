package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.algorithm.ScheduleMonth;
import com.richardbrenkus.shiftschedulermodernized.config.PasswordEncoderConfig;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ScheduleEditForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftAssignmentForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserRegisterForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserUpdateForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserViewRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserValidationResult;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ValidationError;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ScheduleMapper;
import com.richardbrenkus.shiftschedulermodernized.mapper.UserMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoderConfig encoder;
    private final UserMapper userMapper;
    private final UserTransactionalUpdater transactionalUpdater;
    private final UserTransactionalCreator userTransactionalCreator;
    private final ActivityPublisher activityPublisher;
    private final ScheduleMapper scheduleMapper;

    public String getDisplayNameByUserName(String username) {

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid username: " + username);
        }

        User currentUser = userOptional.get();
        String displayName = currentUser.getName();
        if (currentUser.getTitle() != null)
            displayName = currentUser.getTitle() + " " + displayName;

        return displayName;
    }

    public User getUserByUsername(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid username: " + username);
        }

        return userOptional.get();
    }

    @Transactional
    public boolean oldPasswordMatches(String username, String oldPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid username: " + username);
        }

        User user = userOptional.get();

        return encoder.passwordEncoder().matches(oldPassword, user.getPassword());
    }

    @Transactional
    public void changeUserPassword(String username, String newPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid username: " + username);
        }

        User user = userOptional.get();

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

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid username: " + username);
        }

        User user = userOptional.get();

        return user.hasShiftRequest();
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

        return userRepository.findAllByRoleNotOrderByNameAsc(Role.ADMIN);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + userId));
    }

    @Transactional(readOnly = true)
    public String getUsernameByUserId(long userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + userId));
    }

    @Transactional
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
        return userRepository.findByShiftRequestIsNotNullOrderByNameAsc();
    }

    public List<User> getAllUsersAndAdminsByNameAsc() {
        return userRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<UserViewRecord> getAllUserSummaryViewRecordsByNameAsc() {
        return userRepository.findAllByOrderByNameAsc()
                .stream()
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

    @Transactional(readOnly = true)
    public List<UserViewRecord> findAllUsersForSelectionByNameAsc() {

        return userRepository.findAllByOrderByNameAsc()
                .stream()
                .map(userMapper::entityToUserViewRecord)
                .toList();
    }

    public UserViewRecord findUserViewById(Long userId) {

        return userRepository.findById(userId)
                .map(userMapper::entityToUserViewRecord)
                .orElse(null);
    }

    public UserUpdateForm getUserUpdateFormByUserId(long userId) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }

        return userMapper.entityToUserUpdateFormByUserId(userOptional.get());
    }

    @Transactional(readOnly = true)
    public ScheduleMonth getScheduleMonth(ScheduleEditForm scheduleEditForm) {

        Set<Long> ids = scheduleEditForm.getDays()
                .stream()
                .flatMap(day -> day.getAssignments().stream())
                .map(ShiftAssignmentForm::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> usersById = userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return scheduleMapper.toScheduleMonth(scheduleEditForm, scheduleEditForm.toCalculationProfileForm(), usersById);
    }
}
