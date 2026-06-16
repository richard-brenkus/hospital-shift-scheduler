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

    private boolean noShift;
    private boolean anyDate;

    private int shiftCount;
    private int weekendCount;
    private int priority;

    @ElementCollection
    private List<LocalDate> datesYes = new ArrayList<>();

    @ManyToOne
    private ShiftRequest shiftRequest;
}
