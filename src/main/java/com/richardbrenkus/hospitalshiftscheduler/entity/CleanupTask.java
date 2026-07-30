package com.richardbrenkus.hospitalshiftscheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "cleanup_task")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CleanupTask {

    public static final Long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id = SINGLETON_ID;

    @Version
    @Column(nullable = false)
    private Long version;

    private boolean isActive;
    private Instant executionTime;
    private Instant creationTime;
}
