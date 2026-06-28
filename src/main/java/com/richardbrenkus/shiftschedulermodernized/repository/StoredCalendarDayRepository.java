package com.richardbrenkus.shiftschedulermodernized.repository;

import com.richardbrenkus.shiftschedulermodernized.entity.StoredCalendarDay;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface StoredCalendarDayRepository extends CrudRepository<StoredCalendarDay, Long> {

    Collection<StoredCalendarDay> findByMonthYearId(String monthYearId);
}
