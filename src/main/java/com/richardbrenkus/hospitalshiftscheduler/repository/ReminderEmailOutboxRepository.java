package com.richardbrenkus.hospitalshiftscheduler.repository;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.ReminderEmailOutboxStatus;
import com.richardbrenkus.hospitalshiftscheduler.entity.ReminderEmailOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderEmailOutboxRepository
        extends JpaRepository<ReminderEmailOutbox, Long> {

    /**
     * Prevents duplicate outbox jobs for the same reminder occurrence
     * and recipient.
     */
    boolean existsBySourceTaskIdAndScheduledExecutionTimeAndRecipientUserId(Long sourceTaskId, Instant scheduledExecutionTime, Long recipientUserId);

    /**
     * Returns IDs of PENDING or FAILED reminder jobs whose next attempt is due. The caller supplies the permitted statuses.
     **/
    @Query("""
            select outbox.id
            from ReminderEmailOutbox outbox
            where outbox.status in :statuses
              and outbox.nextAttemptAt <= :now
            order by outbox.nextAttemptAt asc,
                     outbox.id asc
            """)
    List<Long> findDispatchableIds(@Param("statuses") List<ReminderEmailOutboxStatus> statuses, @Param("now") Instant now, Pageable pageable);

    /**
     * Claims one outbox row for exclusive processing.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select outbox
            from ReminderEmailOutbox outbox
            where outbox.id = :id
            """)
    Optional<ReminderEmailOutbox> findByIdForUpdate(@Param("id") Long id);

    /**
     * Deletes dispatchable outbox rows for a task in a single statement.
     * Callers use this to cancel pending work when an admin disables the task;
     * PROCESSING rows are left untouched so an in-flight SMTP send can drain.
     */
    @Modifying
    @Query("""
            delete from ReminderEmailOutbox outbox
            where outbox.sourceTaskId = :sourceTaskId
              and outbox.status in (
                  com.richardbrenkus.hospitalshiftscheduler.config.constants.ReminderEmailOutboxStatus.PENDING,
                  com.richardbrenkus.hospitalshiftscheduler.config.constants.ReminderEmailOutboxStatus.FAILED
              )
            """)
    int deleteDispatchableBySourceTaskId(@Param("sourceTaskId") Long sourceTaskId);

    /**
     * Finds PROCESSING jobs whose worker likely crashed.
     */
    @Query("""
            select outbox.id
            from ReminderEmailOutbox outbox
            where outbox.status =
                  com.richardbrenkus.hospitalshiftscheduler.config.constants.ReminderEmailOutboxStatus.PROCESSING
              and outbox.claimedAt <= :staleBefore
            order by outbox.claimedAt asc,
                     outbox.id asc
            """)
    List<Long> findStaleProcessingIds(@Param("staleBefore") Instant staleBefore, Pageable pageable);
}