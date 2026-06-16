package com.richardbrenkus.shiftschedulermodernized.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scheduled_events_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledEventsProfile {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String taskType;
    private Boolean isActive;
    private String finalSubmissionYearMonthDayHourMinuteCode;
    private String yearMonthDayHourMinuteCode;

    private String dateTimeInput;
    private int day;
    private int hour;
    private int minute;
    private int frequencyInDays;
    private int repetitions;
    private String timeStamp;
    private int finalSubmissionDay;
    private int counter;
    private String timeString;
}
