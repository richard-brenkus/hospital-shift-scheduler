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

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean isActive;
    private LocalDateTime startSendingTime;
    private LocalDateTime creationTime;
    private int frequencyInDays;
    private int repetitions;
    private int finalSubmissionDay;
    private int counter;
}
