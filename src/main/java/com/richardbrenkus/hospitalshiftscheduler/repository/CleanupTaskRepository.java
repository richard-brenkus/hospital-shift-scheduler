package com.richardbrenkus.hospitalshiftscheduler.repository;

import com.richardbrenkus.hospitalshiftscheduler.entity.CleanupTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CleanupTaskRepository extends JpaRepository<CleanupTask, Long> {

    //Optional<CleanupTask> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       select task
       from CleanupTask task
       where task.id = :id
       """)
    Optional<CleanupTask> findByIdForUpdate(@Param("id") Long id);

}
