package com.richardbrenkus.shiftschedulermodernized.dto.form;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftPreferenceForm {
    private int shiftType;
    private boolean noShiftRequested;
    private boolean anyDateSelected;
    private int priority;
    private int weekdayCount;
    private int weekendCount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private List<LocalDate> datesYes = new ArrayList<>();


    public String toString(){
        return "ShiftPreferenceForm{" +
                "shiftType=" + shiftType +
                ", noShiftRequested=" + noShiftRequested +
                ", anyDateSelected=" + anyDateSelected +
                ", datesYes=" + datesYes +
                ", priority=" + priority +
                ", weekdayCount=" + weekdayCount +
                ", weekendCount=" + weekendCount +
                '}';
    }
}
