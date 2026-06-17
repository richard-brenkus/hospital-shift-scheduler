package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.dto.view.LandingPageRecord;
import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class LandingPageService {

    private final UserRepository userRepository;

    public LandingPageService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LandingPageRecord getLandingPageRecord() {
        //String requestSubmittedString = "";
        double userCountWithoutAdmin = 0.0;
        double shiftRequestCount = 0.0;
        String percentage = "0%";

        Iterable<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            if (!user.getRole().equals(Role.ADMIN)) {
                userCountWithoutAdmin = userCountWithoutAdmin + 1;

                if (user.isShiftRequestActive()) {
                    shiftRequestCount = shiftRequestCount + 1;
                }
            }
        }

        double onePercent = userCountWithoutAdmin / 100;
        if (userCountWithoutAdmin / 100 != 0.00) {
            percentage = Math.round(shiftRequestCount / onePercent) + "%";
        }

        return new LandingPageRecord(userCountWithoutAdmin, shiftRequestCount, percentage);
    }
}
