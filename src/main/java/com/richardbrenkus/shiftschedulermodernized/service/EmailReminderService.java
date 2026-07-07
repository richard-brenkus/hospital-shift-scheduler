package com.richardbrenkus.shiftschedulermodernized.service;

import org.springframework.stereotype.Service;

@Service
public interface EmailReminderService {

    void sendShiftRequestReminderEmails(int finalSubmissionDay);

}
