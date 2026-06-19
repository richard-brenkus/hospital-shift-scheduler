package com.richardbrenkus.shiftschedulermodernized.dto.form;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ShiftRequestForm {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private List<LocalDate> datesNo = new ArrayList<>();

    private List<ShiftPreferenceForm> preferences = new ArrayList<>();
    private List<Integer> allowedShiftTypes = new ArrayList<>();

    public String toString() {
        return "ShiftRequestForm{" +
                "datesNo=" + datesNo +
                ", preferences=" + preferences.toString() +
                ", allowedShiftTypes=" + allowedShiftTypes +
                '}';
    }
}
