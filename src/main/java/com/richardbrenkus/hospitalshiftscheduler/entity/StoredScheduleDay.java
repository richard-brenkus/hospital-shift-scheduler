package com.richardbrenkus.hospitalshiftscheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "stored_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredScheduleDay {

    @Id
    @Column(name = "date_id")
    private Long dateId;

    @Column(name = "month_year_id", nullable = false)
    private String monthYearId;

    @Column(name = "weekend_or_holiday")
    private boolean weekendOrHoliday;

    @Column(name = "day_integer")
    private Integer dayInteger;

    @ElementCollection
    @CollectionTable(
            name = "stored_schedule_shift_assignments",
            joinColumns = @JoinColumn(name = "date_id")
    )
    @MapKeyColumn(name = "shift_type")
    @Builder.Default
    private Map<Integer, StoredUserSnapshot> assignmentsByShiftType = new HashMap<>();

    public void putAssignment(int shiftType, long userId, String username, String name, String title) {
        assignmentsByShiftType.put(shiftType, new StoredUserSnapshot(userId, username, name, title));
    }
}
