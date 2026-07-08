package com.richardbrenkus.shiftschedulermodernized.repository;

import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoredScheduleDayRepository extends JpaRepository<StoredScheduleDay, Long> {

    List<StoredScheduleDay> findByMonthYearIdOrderByDayIntegerAsc(String monthYearId);

    @Query("select distinct d.monthYearId from StoredScheduleDay d")
    List<String> findDistinctMonthYearIds();

}
