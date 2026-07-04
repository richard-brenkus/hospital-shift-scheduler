package com.richardbrenkus.shiftschedulermodernized.repository;

import com.richardbrenkus.shiftschedulermodernized.entity.UserStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;

public interface UserStatRepository extends JpaRepository<UserStatEntity, Long> {

    List<UserStatEntity> findByYearMonthOrderByShiftTypeAscNameAsc(YearMonth yearMonth);

    void deleteByYearMonth(YearMonth yearMonth);

}