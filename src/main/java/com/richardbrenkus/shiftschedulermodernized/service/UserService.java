package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ApplicationConstants;
import com.richardbrenkus.shiftschedulermodernized.config.PasswordEncoderConfig;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserRegisterForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserUpdateForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserViewRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserUpdateValidationResult;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ValidationError;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.UserMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoderConfig encoder;
    private final PasswordEncoderConfig passwordEncoder;
    private final UserMapper userMapper;
    private final UserTransactionalUpdater transactionalUpdater;

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

        String newEncodedPassword = encoder.passwordEncoder().encode(newPassword);

        user.setPassword(newEncodedPassword);
        userRepository.save(user);
    }

    public boolean hasShiftRequest(String username) {
        User user = userRepository.getUserByUsername(username);
        return user != null && user.hasShiftRequest();
    }

    @Transactional
    public void createUser(UserRegisterForm form) {
        User user = new User();

        user.setCreationDate(ZonedDateTime.now(ApplicationConstants.ZONE_ID));
        user.setName(form.getName().trim());
        user.setUsername(form.getUsername().trim().toLowerCase());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setBirthday(form.getBirthday());
        user.setNote(form.getNote());
        user.setTitle(form.getTitle());
        user.setProfession(form.getProfession());
        user.setEnabled(true);
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.passwordEncoder().encode(form.getPassword()));

        user.setAllowedShiftTypes(new HashSet<>(form.getAllowedShiftTypes()));

        userRepository.save(user);
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
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
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
        userRepository.delete(user);
    }

    public List<User> getAllUsersWithShiftRequestByNameAsc() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .filter(User::hasShiftRequest)
                .sorted(Comparator.comparing(User::getName))
                .toList();
    }

    public List<User> getAllUsersAndAdminsByNameAsc() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(User::getName))
                .toList();
    }

    public List<UserViewRecord> getAllUserSummaryViewRecordsByNameAsc() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(User::getName))
                .map(userMapper::entityToUserViewRecord)
                .toList();
    }

    public UserUpdateValidationResult validateAndUpdateUser(UserUpdateForm updatedUser) {
        UserUpdateValidationResult result = new UserUpdateValidationResult(true, new ArrayList<>(), new ArrayList<>());

        Long id = updatedUser.getId();

        if (!userRepository.existsById(id)) {
            result.addGlobalError(new ValidationError("userNotFound", "The selected user no longer exists."));
            result.setValid(false);
            return result;
        }

        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(updatedUser.getUsername().trim().toLowerCase(), id)) {
            result.addFieldError(new ValidationError("username", "This username already exists! Choose another one."));
        }

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(updatedUser.getEmail().trim().toLowerCase(), id)) {
            result.addFieldError(new ValidationError("email", "This email address already exists! Choose another one."));
        }

        if (userRepository.existsByNameIgnoreCaseAndIdNot(updatedUser.getName().trim(), id)) {
            result.addFieldError(new ValidationError("name", "This name already exists! Choose another one."));
        }

        if (updatedUser.getAllowedShiftTypes() == null || updatedUser.getAllowedShiftTypes().isEmpty()) {
            result.addFieldError(new ValidationError("allowedShiftTypes", "Please select at least one shift type!"));
        }

        if (!result.getFieldErrors().isEmpty() || !result.getGlobalErrors().isEmpty()) {
            result.setValid(false);
            return result;
        }

        try {
            transactionalUpdater.update(updatedUser);
        } catch (ObjectOptimisticLockingFailureException exception) {
            result.addGlobalError(new ValidationError("concurrentUpdate", "This user was modified by another administrator. Reload the page and try again."));
            result.setValid(false);
        } catch (DataIntegrityViolationException exception) {
            result.addGlobalError(new ValidationError("concurrentDuplicate", "The submitted username, name, or email was used by another administrator before your update completed."));
            result.setValid(false);
        }

        return result;
    }

    @Transactional
    public List<UserViewRecord> findAllUsersForSelectionByNameAsc() {

        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
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
        return userMapper.entityToUserUpdateFormByUserId(userId);
    }

}
