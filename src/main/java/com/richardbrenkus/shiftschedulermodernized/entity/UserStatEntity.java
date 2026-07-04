package com.richardbrenkus.shiftschedulermodernized.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.YearMonth;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(
        name = "stored_user_stats",
        indexes = {
                @Index(name = "idx_stored_user_stats_stat_month", columnList = "stat_month"),
                @Index(name = "idx_stored_user_stats_shift_type", columnList = "shift_type"),
                @Index(name = "idx_stored_user_stats_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_month", nullable = false, length = 7)
    private YearMonth yearMonth;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "name")
    private String name;

    @Column(name = "shift_type", nullable = false)
    private int shiftType;

    @Column(name = "requested_weekdays")
    private int requestedWeekdays;

    @Column(name = "requested_weekends")
    private int requestedWeekends;

    @Column(name = "calculated_weekdays")
    private int calculatedWeekdays;

    @Column(name = "calculated_weekends")
    private int calculatedWeekends;

    @Column(name = "remaining_weekdays")
    private int remainingWeekdays;

    @Column(name = "remaining_weekends")
    private int remainingWeekends;

    @Column(name = "any_date_selected")
    private boolean anyDateSelected;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "stored_user_stat_requested_days",
            joinColumns = @JoinColumn(name = "user_stat_id")
    )
    @Column(name = "day_of_month")
    @Builder.Default
    private Set<Integer> requestedDateDays = new TreeSet<>();

    @Column(name = "assigned_weekdays")
    private int assignedWeekdays;

    @Column(name = "assigned_weekends")
    private int assignedWeekends;

    @Column(name = "assigned_total")
    private int assignedTotal;

    @Column(name = "assigned_total_all_shift_types")
    private int assignedTotalAllShiftTypes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "stored_user_stat_assigned_days",
            joinColumns = @JoinColumn(name = "user_stat_id")
    )
    @Column(name = "day_of_month")
    @Builder.Default
    private Set<Integer> assignedDateDays = new TreeSet<>();
}
