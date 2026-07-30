package com.richardbrenkus.shiftschedulermodernized.entity;

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
public class ShiftRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shiftRequestId;

    @OneToMany(mappedBy = "shiftRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShiftPreference> preferences = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "shift_request_dates_no",
            joinColumns = @JoinColumn(name = "shift_request_id")
    )
    @Column(name = "date_no")
    private List<LocalDate> datesNo = new ArrayList<>();
}
