package com.richardbrenkus.shiftschedulermodernized.activity;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import com.richardbrenkus.shiftschedulermodernized.container.AbstractMySqlContainerTest;
import com.richardbrenkus.shiftschedulermodernized.entity.ActivityLog;
import com.richardbrenkus.shiftschedulermodernized.repository.ActivityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the transactional after-commit publication path end to end against
 * a real MySQL Testcontainer. Focuses on:
 *
 * <ul>
 *   <li>Commit publishes exactly one row per event via {@link ActivityLogRepository#insertIfAbsent}.</li>
 *   <li>Rollback of the outer transaction does not produce an activity row
 *       (AFTER_COMMIT contract).</li>
 *   <li>Duplicate {@code eventId} does not create a second row.</li>
 * </ul>
 */
@Sql(statements = "DELETE FROM activity_log", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = "DELETE FROM activity_log", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ActivityLogPersistenceIT extends AbstractMySqlContainerTest {

    @Autowired
    private ActivityPublisher activityPublisher;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ActivityLogWriter activityLogWriter;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Clock applicationClock;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        activityLogRepository.deleteAll();
    }

    @Test
    void insertIfAbsent_shouldOnlyPersistFirstOccurrence_whenSameEventIdUsedTwice() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now(applicationClock);

        int firstResult = insertOnce(eventId, now);
        insertOnce(eventId, now);

        assertThat(firstResult).isEqualTo(1);

        List<ActivityLog> stored = activityLogRepository.findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(0, 10));

        assertThat(stored).extracting(ActivityLog::getEventId).filteredOn(eventId::equals).hasSize(1);
    }

    @Test
    void afterCommit_shouldPersistActivityLog_whenBusinessTransactionCommits() {
        long before = activityLogRepository.count();

        transactionTemplate.executeWithoutResult(status -> activityPublisher.publishSuccess(ActivityType.USER_CREATED, "User", "42", "Created user under a committing transaction"));

        long after = activityLogRepository.count();

        assertThat(after - before).isEqualTo(1L);
    }

    @Test
    void afterCommit_shouldNotPersistActivityLog_whenBusinessTransactionRollsBack() {
        long before = activityLogRepository.count();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            activityPublisher.publishSuccess(ActivityType.USER_CREATED, "User", "42", "Should not be persisted");

            throw new IllegalStateException("simulated business failure");
        })).isInstanceOf(IllegalStateException.class);

        long after = activityLogRepository.count();

        assertThat(after).isEqualTo(before);
    }

    private int insertOnce(UUID eventId, Instant occurredAt) {
        Integer affectedRows = transactionTemplate.execute(status -> activityLogRepository.insertIfAbsent(eventId, ActivityType.USER_CREATED.name(), "alice", Role.ADMIN.name(), "User", "42", "Duplicate protection test", true, null, "POST", "/admin/add", "127.0.0.1", occurredAt));

        if (affectedRows == null) {
            throw new IllegalStateException("Transaction completed without returning the affected-row count");
        }

        return affectedRows;
    }
}