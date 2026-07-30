package com.richardbrenkus.hospitalshiftscheduler.entity;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int shiftType;
    private int priority;
    private int weekdayCount;
    private int weekendCount;

    private boolean noShiftRequested;
    private boolean anyDateSelected;

    @Builder.Default
    @ElementCollection
    private List<LocalDate> datesYes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_request_id", nullable = false)
    private ShiftRequest shiftRequest;

    public String toString(){
        return "ShiftPreference{" +
                "id=" + id +
                ", shiftType=" + shiftType +
                ", priority=" + priority +
                ", weekdayCount=" + weekdayCount +
                ", weekendCount=" + weekendCount +
                ", noShiftRequested=" + noShiftRequested +
                ", anyDateSelected=" + anyDateSelected +
                ", datesYes=" + datesYes +
                '}';
    }

}
