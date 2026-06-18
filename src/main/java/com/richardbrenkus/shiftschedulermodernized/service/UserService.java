package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.PasswordEncoderConfig;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.dto.view.ShiftRequestViewRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.mapper.ShiftRequestMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.richardbrenkus.shiftschedulermodernized.config.ApplicationConstants.DATE_FORMATTER;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ShiftRequestMapper shiftRequestMapper;
    private final PasswordEncoderConfig encoder;

    public UserService(UserRepository userRepository, ShiftRequestMapper shiftRequestMapper, PasswordEncoderConfig encoder) {
        this.userRepository = userRepository;
        this.shiftRequestMapper = shiftRequestMapper;
        this.encoder = encoder;
    }

    public String getDisplayNameByUserName(String userName) {
        User currentUser = userRepository.getUserByUsername(userName);
        String displayName = currentUser.getName();
        if (currentUser.getTitle() != null)
            displayName = currentUser.getTitle() + " " + displayName;

        return displayName;
    }

    public ShiftRequestForm getShiftRequestForm(String username){
        ShiftRequestForm shiftRequestForm = new ShiftRequestForm();
        User currentUser = userRepository.getUserByUsername(username);

        if (currentUser.isShiftRequestActive()) {
            shiftRequestForm = shiftRequestMapper.entityToForm(currentUser.getShiftRequest());
        }

        shiftRequestForm.setAllowedShiftTypes(currentUser.getAllowedShiftTypes());

        return shiftRequestForm;

    }

    public Optional<ShiftRequestViewRecord> getShiftRequestViewRecord(String username) {
        User user = userRepository.getUserByUsername(username);
        if (user == null || user.getShiftRequest() == null || !user.isShiftRequestActive()) {
            return Optional.empty();
        }
        return Optional.of(shiftRequestMapper.entityToViewRecord(user.getShiftRequest(), user));
    }

    public User getUserByUsername(String username) {
        return userRepository.getUserByUsername(username);
    }

    public void changePassword(String username, String newPassword) {
        User user = userRepository.getUserByUsername(username);

        String newEncodedPassword = encoder.passwordEncoder().encode(newPassword);

        user.setPassword(newEncodedPassword);
        userRepository.save(user);
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
