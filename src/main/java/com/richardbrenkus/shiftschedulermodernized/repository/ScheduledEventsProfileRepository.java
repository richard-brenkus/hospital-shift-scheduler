package com.richardbrenkus.shiftschedulermodernized.repository;

import java.util.List;

import com.richardbrenkus.shiftschedulermodernized.entity.ScheduledEventsProfile;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ScheduledEventsProfileRepository extends CrudRepository<ScheduledEventsProfile, Long> {

    @Transactional
    @Query("SELECT s FROM ScheduledEventsProfile s")
    List<ScheduledEventsProfile> findAll(Sort ascending);

    @Transactional
    @Query("SELECT s FROM ScheduledEventsProfile s WHERE s.id = :id")
    ScheduledEventsProfile selectById(@Param("id") Long id);

    @Transactional
    @Query("SELECT s FROM ScheduledEventsProfile s WHERE s.taskType = :taskType")
    ScheduledEventsProfile selectByTaskType(@Param("taskType") String taskType);

    @Modifying
    @Transactional
    @Query("UPDATE ScheduledEventsProfile s SET s.id = :id WHERE s.taskType = :taskType")
    void updateScheduledTaskIdForTaskType(@Param("id") Long id, @Param("taskType") String taskType);

    @Modifying
    @Transactional
    @Query("UPDATE ScheduledEventsProfile s SET s.counter = :counter WHERE s.taskType = :taskType")
    void updateScheduledTaskCounterForTaskType(@Param("counter") int counter, @Param("taskType") String taskType);

    @Modifying
    @Transactional
    @Query("DELETE FROM ScheduledEventsProfile s WHERE s.taskType = :taskType")
    void deleteByTaskType(@Param("taskType") String taskType);
}
