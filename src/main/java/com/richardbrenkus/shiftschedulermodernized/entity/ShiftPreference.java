package com.richardbrenkus.shiftschedulermodernized.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shift_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftPreference {

    @Id
    @GeneratedValue
    private Long id;

    private int shiftType;
    private int priority;
    private int weekdayCount;
    private int weekendCount;

    private boolean noShiftRequested;
    private boolean anyDateSelected;

    @ElementCollection
    private List<LocalDate> datesYes = new ArrayList<>();

    @ManyToOne
    private ShiftRequest shiftRequest;

}
