package com.richardbrenkus.shiftschedulermodernized.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "monthly_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "month_year_id", nullable = false, unique = true)
    private String monthYearId; // e.g. "06/2026"

    @OneToMany(mappedBy = "monthlySchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ScheduleEntry> assignments = new ArrayList<>();
}
