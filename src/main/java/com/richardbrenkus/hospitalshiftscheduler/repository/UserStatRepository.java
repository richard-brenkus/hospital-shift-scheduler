package com.richardbrenkus.hospitalshiftscheduler.repository;

import com.richardbrenkus.hospitalshiftscheduler.entity.UserStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;

@Repository
public interface UserStatRepository extends JpaRepository<UserStatEntity, Long> {

    List<UserStatEntity> findByYearMonthOrderByShiftTypeAscNameAsc(YearMonth yearMonth);

    void deleteByYearMonth(YearMonth yearMonth);

}