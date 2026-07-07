package com.richardbrenkus.shiftschedulermodernized.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
@Slf4j
public class LoggingEmailReminderService implements EmailReminderService {

    @Override
    public void sendShiftRequestReminderEmails(int finalSubmissionDay) {

        log.info("Reminder emails would be sent. Deadline = {}", finalSubmissionDay);

    }
}
