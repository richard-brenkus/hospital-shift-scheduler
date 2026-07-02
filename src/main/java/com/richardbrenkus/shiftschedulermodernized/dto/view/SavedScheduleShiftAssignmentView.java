package com.richardbrenkus.shiftschedulermodernized.dto.view;

import com.richardbrenkus.shiftschedulermodernized.entity.StoredUserSnapshot;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedScheduleShiftAssignmentView {

    private int shiftType;
    private StoredUserSnapshot user;
    private String displayName;
}
