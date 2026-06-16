package com.richardbrenkus.shiftschedulermodernized.entity;


import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftPreferenceForm;
import com.richardbrenkus.shiftschedulermodernized.dto.form.ShiftRequestForm;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shift_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftRequest {

    @Id
    private Long id;

    @OneToOne
    private User user;

    @OneToMany(mappedBy = "shiftRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShiftPreference> preferences = new ArrayList<>();

    @ElementCollection
    private List<LocalDate> datesNo = new ArrayList<>();

    private int shiftCount;
    private int weekendCount;
}
