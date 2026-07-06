package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;


import java.util.List;

@Service
@RequiredArgsConstructor
public class SmtpEmailReminderService implements EmailReminderService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public void sendShiftRequestReminderEmails(int finalSubmissionDay) {
        List<User> usersToRemind = userRepository.findUsersWithoutActiveShiftRequest();

        for (User user : usersToRemind) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }

            sendReminderEmail(user, finalSubmissionDay);
        }
    }

    private void sendReminderEmail(User user, int finalSubmissionDay) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Shift request reminder");
        message.setText(buildMessageText(user, finalSubmissionDay));

        mailSender.send(message);
    }

    private String buildMessageText(User user, int finalSubmissionDay) {
        String name = user.getName() == null || user.getName().isBlank()
                ? user.getUsername()
                : user.getName();

        return """
                Hello %s,

                this is a reminder to submit your shift request.

                Please submit your request by day %d of the month.

                Thank you.
                """.formatted(name, finalSubmissionDay);
    }
}
