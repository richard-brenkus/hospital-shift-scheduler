package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getDisplayNameByUserName(String userName) {
        User currentUser = userRepository.getUserByUsername(userName);
        String displayName = currentUser.getName();
        if (currentUser.getTitle() != null)
            displayName = currentUser.getTitle() + " " + displayName;

        return displayName;
    }

    public ShiftRequestForm getShiftRequestForm(String username){
        ShiftRequestForm shiftRequestForm = ShiftRequestFormFactory.createEmptyShiftRequestForm();
        User currentUser = userRepository.getUserByUsername(username);

        if (currentUser.getHasShiftRequest()) {
            shiftRequestForm = new ShiftRequestForm(currentUser.getShiftRequest());
        }

        shiftRequestForm.setShiftType1(currentUser.getShiftType1());
        shiftRequestForm.setShiftType2(currentUser.getShiftType2());
        shiftRequestForm.setShiftType3(currentUser.getShiftType3());
        shiftRequestForm.setShiftType4(currentUser.getShiftType4());
        shiftRequestForm.setShiftType5(currentUser.getShiftType5());
        shiftRequestForm.setShiftType6(currentUser.getShiftType6());

        return shiftRequestForm;

    }

    public Optional<SubmittedShiftRequestRecord> getSubmittedShiftRequestRecord(String username) {
        User user = userRepository.getUserByUsername(username);
        if (user == null || user.getShiftRequest() == null || !user.getHasShiftRequest()) {
            return Optional.empty();
        }
        return Optional.of(new SubmittedShiftRequestRecord(user.getShiftRequest()));
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
