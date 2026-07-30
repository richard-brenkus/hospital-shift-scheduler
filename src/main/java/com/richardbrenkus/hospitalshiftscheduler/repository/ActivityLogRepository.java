package com.richardbrenkus.hospitalshiftscheduler.repository;

import com.richardbrenkus.hospitalshiftscheduler.entity.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);

    @Modifying
    @Query(
            value = """
                    INSERT INTO activity_log (
                        event_id,
                        activity_type,
                        actor_username,
                        actor_role,
                        target_type,
                        target_id,
                        description,
                        successful,
                        failure_reason,
                        request_method,
                        request_path,
                        client_ip,
                        occurred_at
                    )
                    VALUES (
                        :eventId,
                        :activityType,
                        :actorUsername,
                        :actorRole,
                        :targetType,
                        :targetId,
                        :description,
                        :successful,
                        :failureReason,
                        :requestMethod,
                        :requestUri,
                        :clientIp,
                        :occurredAt
                    )
                    ON DUPLICATE KEY UPDATE event_id = event_id
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("activityType") String activityType,
            @Param("actorUsername") String actorUsername,
            @Param("actorRole") String actorRole,
            @Param("targetType") String targetType,
            @Param("targetId") String targetId,
            @Param("description") String description,
            @Param("successful") boolean successful,
            @Param("failureReason") String failureReason,
            @Param("requestMethod") String requestMethod,
            @Param("requestUri") String requestUri,
            @Param("clientIp") String clientIp,
            @Param("occurredAt") Instant occurredAt
    );
}