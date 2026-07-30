package com.richardbrenkus.hospitalshiftscheduler.entity;

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
    private boolean isTaskActive;
    private String finalSubmissionYearMonthDayHourMinuteCode;
    private String yearMonthDayHourMinuteCode;

    private String dateTimeInput;

    @Column(name = "event_counter")
    private int counter;

    @Column(name = "event_day")
    private int day;

    @Column(name = "event_month")
    private int month;

    @Column(name = "event_year")
    private int year;

    @Column(name = "event_hour")
    private int hour;

    @Column(name = "event_minute")
    private int minute;

    private int frequencyInDays;
    private int repetitions;
    private String timeStamp;
    private int finalSubmissionDay;
    private String timeString;
}
