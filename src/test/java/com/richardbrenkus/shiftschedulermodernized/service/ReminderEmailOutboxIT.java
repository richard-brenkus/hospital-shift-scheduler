package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ReminderEmailOutboxStatus;
import com.richardbrenkus.shiftschedulermodernized.container.AbstractMySqlContainerTest;
import com.richardbrenkus.shiftschedulermodernized.entity.ReminderEmailOutbox;
import com.richardbrenkus.shiftschedulermodernized.repository.ReminderEmailOutboxRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies persistence-level concerns against a real MySQL container:
 * <ul>
 *   <li>uniqueness constraints prevent duplicate rows for the same
 *       (task, occurrence, recipient) tuple;</li>
 *   <li>{@code findDispatchableIds} returns rows in the documented order and
 *       excludes non-dispatchable statuses;</li>
 *   <li>pessimistic-lock-driven claim serialises two workers competing for the
 *       same row.</li>
 * </ul>
 */
class ReminderEmailOutboxIT extends AbstractMySqlContainerTest {

    private static final Instant NOW = Instant.parse("2026-08-05T08:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-08-20T21:59:59Z");
    private static final LocalDate FINAL_DAY = LocalDate.of(2026, 8, 20);

    @Autowired
    private ReminderEmailOutboxRepository repository;

    @Autowired
    private ReminderEmailOutboxClaimService claimService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        repository.deleteAll();
    }

    @Test
    void save_shouldRejectDuplicate_whenSameTaskOccurrenceRecipient() {
        transactionTemplate.executeWithoutResult(status -> repository.saveAndFlush(pendingJob(1L, 99L)));

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> repository.saveAndFlush(pendingJob(1L, 99L)))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findDispatchableIds_shouldExcludeSentAndDeadRowsAndOrderByNextAttempt() {
        transactionTemplate.executeWithoutResult(status -> {
            ReminderEmailOutbox first = pendingJob(1L, 10L);
            ReflectionTestUtils.setField(first, "nextAttemptAt", NOW.minusSeconds(100));
            ReminderEmailOutbox second = pendingJob(1L, 11L);
            ReflectionTestUtils.setField(second, "nextAttemptAt", NOW.minusSeconds(50));
            ReminderEmailOutbox future = pendingJob(1L, 12L);
            ReflectionTestUtils.setField(future, "nextAttemptAt", NOW.plusSeconds(60));

            ReminderEmailOutbox sent = pendingJob(1L, 13L);
            sent.claim("worker-a", "token-a", NOW);
            sent.markSent("token-a", NOW.plusSeconds(1));

            ReminderEmailOutbox dead = pendingJob(1L, 14L);
            dead.claim("worker-b", "token-b", NOW);
            dead.markDead("token-b", "permanent", NOW.plusSeconds(1));

            repository.saveAll(List.of(first, second, future, sent, dead));
            repository.flush();
        });

        List<Long> due = repository.findDispatchableIds(List.of(ReminderEmailOutboxStatus.PENDING, ReminderEmailOutboxStatus.FAILED), NOW, PageRequest.of(0, 10));

        // future / sent / dead must not appear; first two are ordered by nextAttemptAt.
        assertThat(due).hasSize(2);
        // We cannot reference the auto-generated ids beforehand, so verify by re-loading.
        List<ReminderEmailOutbox> ordered = due.stream().map(id -> repository.findById(id).orElseThrow()).toList();
        assertThat(ordered.get(0).getRecipientUserId()).isEqualTo(10L);
        assertThat(ordered.get(1).getRecipientUserId()).isEqualTo(11L);
    }

    @Test
    void claim_shouldSerializeTwoWorkersCompetingForTheSameRow() {
        Long jobId = transactionTemplate.execute(status -> {
            ReminderEmailOutbox saved = repository.saveAndFlush(pendingJob(1L, 99L));
            return saved.getId();
        });

        var first = claimService.claim(jobId, "worker-1", NOW);
        var second = claimService.claim(jobId, "worker-2", NOW);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();

        Assertions.assertNotNull(jobId);
        ReminderEmailOutbox reloaded = repository.findById(jobId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ReminderEmailOutboxStatus.PROCESSING);
        assertThat(reloaded.getClaimedBy()).isEqualTo("worker-1");
        assertThat(reloaded.getClaimToken()).isEqualTo(first.get().claimToken());
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void insertIfAbsent_semantics_areEnforcedByUniqueIndex() {
        // Direct-save path also relies on the unique index. Ensure two saves for
        // the same idempotency key are rejected by the database, not silently
        // absorbed.
        ReminderEmailOutbox first = transactionTemplate.execute(status -> repository.saveAndFlush(pendingJob(1L, 42L)));
        // Craft a second row with the same idempotency key value.
        ReminderEmailOutbox second = pendingJob(1L, 43L);
        Assertions.assertNotNull(first);
        ReflectionTestUtils.setField(second, "idempotencyKey", first.getIdempotencyKey());

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> repository.saveAndFlush(second))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsBySourceTaskIdAndScheduledExecutionTimeAndRecipientUserId_shouldReturnTrue_whenRowExists() {
        transactionTemplate.executeWithoutResult(status -> repository.saveAndFlush(pendingJob(1L, 42L)));

        boolean present = repository.existsBySourceTaskIdAndScheduledExecutionTimeAndRecipientUserId(1L, NOW, 42L);
        boolean absent = repository.existsBySourceTaskIdAndScheduledExecutionTimeAndRecipientUserId(1L, NOW, 999L);

        assertThat(present).isTrue();
        assertThat(absent).isFalse();
    }

    @Test
    void findByIdForUpdate_shouldReturnRow_whenPresent() {
        Long id = transactionTemplate.execute(status -> repository.saveAndFlush(pendingJob(1L, 42L)).getId());

        Optional<ReminderEmailOutbox> found = transactionTemplate.execute(status -> repository.findByIdForUpdate(id));

        assertThat(found).isPresent();
    }

    private static ReminderEmailOutbox pendingJob(long taskId, long recipientId) {
        return ReminderEmailOutbox.pending(taskId, NOW, FINAL_DAY, DEADLINE, recipientId, "user-" + recipientId + "@example.test", "User " + recipientId, NOW);
    }
}
