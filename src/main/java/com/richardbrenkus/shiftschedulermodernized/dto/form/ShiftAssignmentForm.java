package com.richardbrenkus.shiftschedulermodernized.dto.form;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftAssignmentForm {

    private int shiftType;
    private Long userId;
    private String username;
    private String title;
    private String name;

}
