package com.richardbrenkus.shiftschedulermodernized.repository;

import com.richardbrenkus.shiftschedulermodernized.entity.StoredCalendarDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoredCalendarDayRepository extends JpaRepository<StoredCalendarDay, Long> {

    List<StoredCalendarDay> findByMonthYearIdOrderByDayIntegerAsc(String monthYearId);

    @Query("select distinct d.monthYearId from StoredCalendarDay d")
    List<String> findDistinctMonthYearIds();

}
