package com.richardbrenkus.shiftschedulermodernized.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "send_reminder_task")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendReminderTask {

    public static final Long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    private boolean isActive;
    private LocalDateTime startSendingTime;
    private LocalDateTime creationTime;
    private int frequencyInDays;
    private int repetitions;
    private LocalDateTime finalRequestSubmissionDate;
    private int counter;
}
