package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.exception.PermanentEmailDeliveryException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/*
 * NOTE: This test was regenerated after the SMTP reminder service was
 * refactored to the per-recipient outbox model. Bulk-scan tests referencing
 * the removed user-scanning behavior were replaced with per-recipient tests.
 */
@ExtendWith(MockitoExtension.class)
class SmtpEmailReminderServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SmtpEmailReminderService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "fromAddress", "sender@example.test");
        ReflectionTestUtils.setField(service, "messageIdDomain", "test.example");
    }

    @Test
    void shouldSendOneMessage_perValidInvocation() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        service.sendShiftRequestReminderEmail(
                "recipient@example.test",
                "Recipient",
                LocalDate.now().plusDays(5),
                "idem-1"
        );

        verify(mailSender).send((MimeMessage) ArgumentCaptor.forClass(MimeMessage.class).capture());
    }

    @Test
    void shouldRejectBlankRecipientEmail() {
        assertThatThrownBy(() -> service.sendShiftRequestReminderEmail(
                "",
                "Recipient",
                LocalDate.now().plusDays(5),
                "idem-2"
        )).isInstanceOf(PermanentEmailDeliveryException.class);
        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldRejectNullFinalSubmissionDate() {
        assertThatThrownBy(() -> service.sendShiftRequestReminderEmail(
                "recipient@example.test",
                "Recipient",
                null,
                "idem-3"
        )).isInstanceOf(PermanentEmailDeliveryException.class);
        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {
        assertThatThrownBy(() -> service.sendShiftRequestReminderEmail(
                "recipient@example.test",
                "Recipient",
                LocalDate.now().plusDays(5),
                ""
        )).isInstanceOf(PermanentEmailDeliveryException.class);
        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldNotThrow_whenAllArgumentsValid() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        assertThatCode(() -> service.sendShiftRequestReminderEmail(
                "recipient@example.test",
                "Recipient Name",
                LocalDate.now().plusDays(10),
                "idem-4"
        )).doesNotThrowAnyException();
    }
}
