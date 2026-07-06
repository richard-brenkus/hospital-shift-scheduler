package com.richardbrenkus.shiftschedulermodernized.repository;

import com.richardbrenkus.shiftschedulermodernized.entity.CleanupTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CleanupTaskRepository extends JpaRepository<CleanupTask, Long> {

    @Transactional
    @Query("SELECT s FROM CleanupTask s WHERE s.isActive = true")
    List<CleanupTask> findByIsActive();

    Optional<CleanupTask> findFirstByIsActiveTrueOrderByExecutionTimeAsc();

}
