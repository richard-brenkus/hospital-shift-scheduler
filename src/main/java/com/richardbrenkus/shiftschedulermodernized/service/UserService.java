package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.ApplicationConstants;
import com.richardbrenkus.shiftschedulermodernized.config.PasswordEncoderConfig;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftPreferenceForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.UserForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ShiftRequestViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ShiftRequestMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.ShiftRequestRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static com.richardbrenkus.shiftschedulermodernized.config.ApplicationConstants.DATE_FORMATTER;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ShiftRequestMapper shiftRequestMapper;
    private final PasswordEncoderConfig encoder;
    private final ShiftRequestRepository shiftRequestRepository;
    private final PasswordEncoderConfig passwordEncoder;

    public UserService(UserRepository userRepository, ShiftRequestMapper shiftRequestMapper, PasswordEncoderConfig encoder, ShiftRequestRepository shiftRequestRepository, PasswordEncoderConfig passwordEncoder) {
        this.userRepository = userRepository;
        this.shiftRequestMapper = shiftRequestMapper;
        this.encoder = encoder;
        this.shiftRequestRepository = shiftRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String getDisplayNameByUserName(String userName) {
        User currentUser = userRepository.getUserByUsername(userName);
        String displayName = currentUser.getName();
        if (currentUser.getTitle() != null)
            displayName = currentUser.getTitle() + " " + displayName;

        return displayName;
    }

    /*public UserForm getUserFormByUserId(long userId){
        UserForm userForm = new UserForm();
        User currentUser = userRepository.getUserById(userId);

        if (currentUser.getShiftRequest() != null) {
            //userForm = shiftRequestMapper.entityToForm(currentUser.getShiftRequest());
        }
        else
            this.fillAllowedShiftTypes(currentUser, userForm);

        return userForm;
    }*/



    public User getUserByUsername(String username) {
        return userRepository.getUserByUsername(username);
    }

    public void changePassword(String username, String newPassword) {
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

    public List<User> getAllUsersWithoutAdmin() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .filter(user -> !user.isAdmin())
                .toList();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + userId));
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    public List<User> getAllUsersWithShiftRequest() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .filter(User::hasShiftRequest)
                .toList();
    }

    public List<User> getAllUsersAndAdmins(){
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .toList();
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
