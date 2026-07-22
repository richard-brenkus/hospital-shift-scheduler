package com.richardbrenkus.shiftschedulermodernized.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cleanup_task")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CleanupTask {

    public static final Long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    private boolean isActive;
    private LocalDateTime executionTime;
    private LocalDateTime creationTime;
}
