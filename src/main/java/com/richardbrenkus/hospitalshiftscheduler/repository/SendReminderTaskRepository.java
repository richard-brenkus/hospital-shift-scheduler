package com.richardbrenkus.hospitalshiftscheduler.repository;

import com.richardbrenkus.hospitalshiftscheduler.entity.SendReminderTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SendReminderTaskRepository extends JpaRepository<SendReminderTask, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       select task
       from SendReminderTask task
       where task.id = :id
       """)
    Optional<SendReminderTask> findByIdForUpdate(@Param("id") Long id);

}
