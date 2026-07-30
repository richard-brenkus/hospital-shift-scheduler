package com.richardbrenkus.hospitalshiftscheduler.repository;

import com.richardbrenkus.hospitalshiftscheduler.entity.StoredScheduleDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoredScheduleDayRepository extends JpaRepository<StoredScheduleDay, Long> {

    List<StoredScheduleDay> findByMonthYearIdOrderByDayIntegerAsc(String monthYearId);

    boolean existsByMonthYearId(String monthYearId);
}
