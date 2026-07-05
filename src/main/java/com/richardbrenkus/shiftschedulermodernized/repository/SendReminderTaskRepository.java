package com.richardbrenkus.shiftschedulermodernized.repository;

import com.richardbrenkus.shiftschedulermodernized.entity.SendReminderTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SendReminderTaskRepository extends JpaRepository<SendReminderTask, Long> {

    @Transactional
    @Query("SELECT s FROM CleanupTask s WHERE s.isActive = true")
    List<SendReminderTask> findByIsActive();

}
