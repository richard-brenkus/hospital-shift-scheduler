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

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean isActive;
    private LocalDateTime executionTime;
    private LocalDateTime creationTime;
}
