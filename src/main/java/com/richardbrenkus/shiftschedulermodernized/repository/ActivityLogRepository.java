package com.richardbrenkus.shiftschedulermodernized.repository;

import com.richardbrenkus.shiftschedulermodernized.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
}
