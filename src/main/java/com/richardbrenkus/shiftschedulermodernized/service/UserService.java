package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.ApplicationConstants;
import com.richardbrenkus.shiftschedulermodernized.config.PasswordEncoderConfig;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserViewRecord;
import com.richardbrenkus.shiftschedulermodernized.dto.view.UserUpdateValidationResult;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ShiftRequestMapper;
import com.richardbrenkus.shiftschedulermodernized.mapper.UserMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.ShiftRequestRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.StreamSupport;

import static com.richardbrenkus.shiftschedulermodernized.config.ApplicationConstants.DATE_FORMATTER;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ShiftRequestMapper shiftRequestMapper;
    private final PasswordEncoderConfig encoder;
    private final ShiftRequestRepository shiftRequestRepository;
    private final PasswordEncoderConfig passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, ShiftRequestMapper shiftRequestMapper, PasswordEncoderConfig encoder, ShiftRequestRepository shiftRequestRepository, PasswordEncoderConfig passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.shiftRequestMapper = shiftRequestMapper;
        this.encoder = encoder;
        this.shiftRequestRepository = shiftRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    public String getDisplayNameByUserName(String userName) {
        User currentUser = userRepository.getUserByUsername(userName);
        String displayName = currentUser.getName();
        if (currentUser.getTitle() != null)
            displayName = currentUser.getTitle() + " " + displayName;

        return displayName;
    }

    public UserForm getUserFormByUserId(long userId) {
        return userMapper.entityToUserFormByUserId(userId);
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
    public User createUser(UserForm form) {
        User user = new User();

        user.setCreationDate(ZonedDateTime.now(ApplicationConstants.ZONE_ID));
        user.setName(form.getName());
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setBirthday(form.getBirthday());
        user.setNote(form.getNote());
        user.setTitle(form.getTitle());
        user.setProfession(form.getProfession());
        user.setEnabled(true);
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.passwordEncoder().encode(form.getPassword()));

        user.setAllowedShiftTypes(new HashSet<>(form.getAllowedShiftTypes()));

        return userRepository.save(user);
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

    @Transactional
    public void updateUser(UserForm form) {

        User existingUser = userRepository.findById(form.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + form.getId()));

        existingUser.setTitle(form.getTitle());
        existingUser.setName(form.getName());
        existingUser.setUsername(form.getUsername());
        existingUser.setBirthday(form.getBirthday());
        existingUser.setEmail(form.getEmail());
        existingUser.setNote(form.getNote());
        existingUser.setProfession(form.getProfession());
        existingUser.setAllowedShiftTypes(form.getAllowedShiftTypes() == null
                ? new HashSet<>()
                : new HashSet<>(form.getAllowedShiftTypes()));
        userRepository.save(existingUser);
        // a role change here is not allowed as it is database-only
        // a password change is handled separately
    }


    public UserUpdateValidationResult validateUserUpdate(UserForm updatedUser) {
        Long id = updatedUser.getId();

        List<String> defaultMessages = new ArrayList<>();
        List<String> rejectedFields = new ArrayList<>();

        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(updatedUser.getUsername(), id)) {
            defaultMessages.add("This username already exists! Choose another one.");
            rejectedFields.add("username");
        }

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(updatedUser.getEmail(), id)) {
            defaultMessages.add("This email address already exists! Choose another one.");
            rejectedFields.add("email");
        }

        if (userRepository.existsByNameIgnoreCaseAndIdNot(updatedUser.getName(), id)) {
            defaultMessages.add("This name already exists! Choose another one.");
            rejectedFields.add("name");
        }

        if (updatedUser.getAllowedShiftTypes() == null || updatedUser.getAllowedShiftTypes().isEmpty()) {
            defaultMessages.add("Please select at least one shift type!");
            rejectedFields.add("allowedShiftTypes");
        }

        return new UserUpdateValidationResult(rejectedFields.isEmpty(), defaultMessages, rejectedFields);
    }

    public UserViewRecord entityToUserViewRecord(User user) {
        return new UserViewRecord(user.getId(), user.getName(), user.getUsername(), user.getEmail());
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

    private String getShiftRequestDatesAsString(List<LocalDate> localDatesList) {

        List<String> stringDatesList = new ArrayList<>();
        String stringDates = "";
        if (localDatesList != null && !localDatesList.isEmpty()) {
            for (LocalDate d : localDatesList) {
                stringDatesList.add(DATE_FORMATTER.format(d));
            }
            stringDates = stringDatesList.toString();
            stringDates = stringDates.substring(1, stringDates.length() - 1);
        }

        return stringDates;
    }


}
