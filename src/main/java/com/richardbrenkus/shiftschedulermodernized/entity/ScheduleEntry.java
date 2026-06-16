package com.richardbrenkus.shiftschedulermodernized.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "schedule_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_of_month", nullable = false)
    private int dayOfMonth;

    @Column(name = "shift_type", nullable = false)
    private int shiftType;

    @Column(name = "weekend_or_holiday", nullable = false)
    private boolean weekendOrHoliday;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "user_title")
    private String userTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_schedule_id", nullable = false)
    private MonthlySchedule monthlySchedule;
}
