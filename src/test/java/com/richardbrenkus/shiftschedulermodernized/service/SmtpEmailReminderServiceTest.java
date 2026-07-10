package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.entity.User;
import com.richardbrenkus.shiftschedulermodernized.repository.UserRepository;
import com.richardbrenkus.shiftschedulermodernized.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailReminderServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SmtpEmailReminderService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "fromAddress", "sender@example.test");
    }

    @Test
    void shouldSendOneMessagePerEligibleUser() {
        User first = TestFixtures.user(1L, "freddie");
        first.setEmail("freddie@example.test");
        User second = TestFixtures.user(2L, "mick");
        second.setEmail("mick@example.test");
        when(userRepository.findUsersWithoutActiveShiftRequest()).thenReturn(List.of(first, second));

        service.sendShiftRequestReminderEmails(20);

        verify(mailSender, times(2)).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void shouldNotSendMessage_whenUserEmailIsNull() {
        User user = TestFixtures.user(1L, "freddie");
        user.setEmail(null);
        when(userRepository.findUsersWithoutActiveShiftRequest()).thenReturn(List.of(user));

        service.sendShiftRequestReminderEmails(20);

        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldNotSendMessage_whenUserEmailIsBlank() {
        User user = TestFixtures.user(1L, "freddie");
        user.setEmail("   ");
        when(userRepository.findUsersWithoutActiveShiftRequest()).thenReturn(List.of(user));

        service.sendShiftRequestReminderEmails(20);

        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldNotSendAnyMessage_whenNoUsersMatch() {
        when(userRepository.findUsersWithoutActiveShiftRequest()).thenReturn(List.of());

        service.sendShiftRequestReminderEmails(20);

        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void shouldPopulateSenderRecipientSubjectAndBody() {
        User user = TestFixtures.user(1L, "freddie");
        user.setEmail("freddie@example.test");
        user.setName("Freddie Mercury");
        when(userRepository.findUsersWithoutActiveShiftRequest()).thenReturn(List.of(user));

        service.sendShiftRequestReminderEmails(15);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("sender@example.test");
        assertThat(sent.getTo()).containsExactly("freddie@example.test");
        assertThat(sent.getSubject()).isEqualTo("Shift request reminder");
        assertThat(sent.getText())
                .contains("Freddie Mercury")
                .contains("day 15");
    }

    @Test
    void shouldUseUsername_whenNameIsBlank() {
        User user = TestFixtures.user(1L, "freddie");
        user.setEmail("freddie@example.test");
        user.setName("");
        when(userRepository.findUsersWithoutActiveShiftRequest()).thenReturn(List.of(user));

        service.sendShiftRequestReminderEmails(15);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getText()).contains("freddie");
    }
}
