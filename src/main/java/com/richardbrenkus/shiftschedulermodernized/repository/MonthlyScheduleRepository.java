package com.richardbrenkus.shiftschedulermodernized.repository;

import com.richardbrenkus.shiftschedulermodernized.entity.MonthlySchedule;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface MonthlyScheduleRepository extends CrudRepository<MonthlySchedule, Long> {

    @Query("SELECT c FROM MonthlySchedule c WHERE c.dateId = :dateId")
    MonthlySchedule getCalendarDayByDateId(@Param("dateId") Integer dateId);

    @Query("SELECT c FROM MonthlySchedule c WHERE c.monthYearId = :monthYearId")
    Collection<MonthlySchedule> getCalendarByDateMonthYearId(@Param("monthYearId") String monthYearId);
}
