package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityPublisher;
import com.richardbrenkus.shiftschedulermodernized.dto.form.CleanupTaskForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.SendReminderTaskForm;
import com.richardbrenkus.shiftschedulermodernized.mapper.PlannedTaskMapper;
import com.richardbrenkus.shiftschedulermodernized.repository.CleanupTaskRepository;
import com.richardbrenkus.shiftschedulermodernized.repository.SendReminderTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * NOTE: The original generated tests were structurally obsolete after
 * PlannedTasksService switched to the singleton-row + pessimistic-lock model
 * and Instant/ZoneId-based time handling. The behaviour that used to be
 * asserted (findAll-based deactivation cascades, LocalDateTime execution
 * times, no Clock injection) no longer exists in production. The
 * form-validation helpers below still test meaningful current behaviour.
 */
@ExtendWith(MockitoExtension.class)
class PlannedTasksServiceTest {

    @Mock
    private CleanupTaskRepository cleanupTaskRepository;

    @Mock
    private SendReminderTaskRepository sendReminderTaskRepository;

    @Mock
    private ActivityPublisher activityPublisher;

    @Mock
    private PlannedTaskMapper plannedTaskMapper;

    private final ZoneId zone = ZoneId.of("Europe/Prague");
    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-08-05T08:00:00Z"),
            zone
    );

    private PlannedTasksService service;

    @BeforeEach
    void setUp() {
        service = new PlannedTasksService(
                cleanupTaskRepository,
                sendReminderTaskRepository,
                activityPublisher,
                plannedTaskMapper,
                fixedClock,
                zone
        );
    }

    @Test
    void shouldReturnDayError_whenReminderDayIsGreaterThanOrEqualToFinalSubmissionDay() {
        SendReminderTaskForm form = SendReminderTaskForm.builder()
                .isSendReminderTaskActive(true)
                .startSendingRemindersDay(21)
                .finalSubmissionDay(20)
                .build();

        assertThat(service.hasDayError(form)).isTrue();
    }

    @Test
    void shouldNotReturnDayError_whenReminderDayIsBeforeFinalSubmissionDay() {
        SendReminderTaskForm form = SendReminderTaskForm.builder()
                .isSendReminderTaskActive(true)
                .startSendingRemindersDay(15)
                .finalSubmissionDay(20)
                .build();

        assertThat(service.hasDayError(form)).isFalse();
    }

    @Test
    void shouldNotReturnDayError_whenReminderIsInactive() {
        SendReminderTaskForm form = SendReminderTaskForm.builder()
                .isSendReminderTaskActive(false)
                .startSendingRemindersDay(25)
                .finalSubmissionDay(20)
                .build();

        assertThat(service.hasDayError(form)).isFalse();
    }

    @Test
    void shouldReturnTrue_whenCleanupIsInactive() {
        CleanupTaskForm form = CleanupTaskForm.builder()
                .isCleanupTaskActive(false)
                .build();

        assertThat(service.isCleanupTimeInFuture(form, Instant.parse("2026-08-05T08:00:00Z")))
                .isTrue();
    }

    @Test
    void shouldReturnTrue_whenSendReminderIsInactive() {
        SendReminderTaskForm form = SendReminderTaskForm.builder()
                .isSendReminderTaskActive(false)
                .build();

        Instant now = Instant.parse("2026-08-05T08:00:00Z");

        assertThat(service.isFirstReminderInFuture(form, now)).isTrue();
        assertThat(service.isSendRemindersSetupValid(form, now)).isTrue();
    }
}
