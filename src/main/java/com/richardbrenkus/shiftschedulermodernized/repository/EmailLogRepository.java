package com.richardbrenkus.shiftschedulermodernized.repository;

import com.richardbrenkus.shiftschedulermodernized.entity.EmailLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface EmailLogRepository extends CrudRepository<EmailLog, Long> {

    @Query("SELECT e FROM EmailLog e WHERE e.monthYearString = :monthYearString")
    EmailLog getEmailLogByMonthYearString(@Param("monthYearString") String monthYearString);

}
