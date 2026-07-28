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

        // insertIfAbsent is a @Modifying native query; JPA requires an active
        // transaction. Production calls it from ActivityLogWriter.persist,
        // which is @Transactional(REQUIRES_NEW).
        int firstInsert = insertOnce(eventId, now);
        int secondInsert = insertOnce(eventId, now);

        assertThat(firstInsert).isEqualTo(1);
        assertThat(secondInsert).isEqualTo(0); // ON DUPLICATE KEY UPDATE → 0 rows affected

        List<ActivityLog> stored = activityLogRepository.findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(0, 10));
        assertThat(stored)
                .extracting(ActivityLog::getEventId)
                .filteredOn(id -> id.equals(eventId))
                .hasSize(1);
    }

    @Test
    void afterCommit_shouldPersistActivityLog_whenBusinessTransactionCommits() {
        long before = activityLogRepository.count();

        transactionTemplate.executeWithoutResult(status ->
                activityPublisher.publishSuccess(
                        ActivityType.USER_CREATED,
                        "User",
                        "42",
                        "Created user under a committing transaction"
                )
        );

        long after = activityLogRepository.count();
        assertThat(after - before).isEqualTo(1L);
    }

    @Test
    void afterCommit_shouldNotPersistActivityLog_whenBusinessTransactionRollsBack() {
        long before = activityLogRepository.count();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            activityPublisher.publishSuccess(
                    ActivityType.USER_CREATED,
                    "User",
                    "42",
                    "Should not be persisted"
            );
            throw new IllegalStateException("simulated business failure");
        })).isInstanceOf(IllegalStateException.class);

        long after = activityLogRepository.count();
        assertThat(after).isEqualTo(before);
    }

    private int insertOnce(UUID eventId, Instant occurredAt) {
        Integer affected = transactionTemplate.execute(status ->
                activityLogRepository.insertIfAbsent(
                        eventId,
                        ActivityType.USER_CREATED.name(),
                        "alice",
                        Role.ADMIN.name(),
                        "User",
                        "42",
                        "Duplicate protection test",
                        true,
                        null,
                        "POST",
                        "/admin/add",
                        "127.0.0.1",
                        occurredAt
                )
        );
        return affected == null ? 0 : affected;
    }
}
