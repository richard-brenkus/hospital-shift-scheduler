package com.richardbrenkus.shiftschedulermodernized.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredShiftAssignment {

    @Column(name = "username")
    private String username;

    @Column(name = "user_title")
    private String title;
}