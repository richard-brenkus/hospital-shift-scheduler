package com.richardbrenkus.shiftschedulermodernized.entity;

import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftPreferenceForm;
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
    private int shiftPriority;

    private boolean shiftUnwanted;
    private boolean anyDateSelectionAllowed;

    @ElementCollection
    private List<LocalDate> datesYes = new ArrayList<>();

    @ManyToOne
    private ShiftRequest shiftRequest;

    public ShiftPreference(ShiftPreferenceForm shiftPreferenceForm) {
        this.shiftType = shiftPreferenceForm.getShiftType();
        this.shiftUnwanted = shiftPreferenceForm.isShiftUnwanted();
        this.anyDateSelectionAllowed = shiftPreferenceForm.isAnyDateSelectionAllowed();
        this.datesYes = shiftPreferenceForm.getDatesYes();
    }
}
