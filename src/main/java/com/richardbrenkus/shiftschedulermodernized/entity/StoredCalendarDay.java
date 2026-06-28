package com.richardbrenkus.shiftschedulermodernized.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "stored_calendars")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredCalendarDay {

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
            name = "stored_calendar_shift_assignments",
            joinColumns = @JoinColumn(name = "date_id")
    )
    @MapKeyColumn(name = "shift_type")
    @Builder.Default
    private Map<Integer, StoredShiftAssignment> assignmentsByShiftType = new HashMap<>();

    public String getUsernameForShiftType(int shiftType) {
        StoredShiftAssignment assignment = assignmentsByShiftType.get(shiftType);
        return assignment == null ? "" : assignment.getUsername();
    }

    public String getTitleForShiftType(int shiftType) {
        StoredShiftAssignment assignment = assignmentsByShiftType.get(shiftType);
        return assignment == null ? "" : assignment.getTitle();
    }

    public void putAssignment(int shiftType, String username, String title) {
        assignmentsByShiftType.put(
                shiftType,
                new StoredShiftAssignment(username, title)
        );
    }
}
