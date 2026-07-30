package com.richardbrenkus.hospitalshiftscheduler.dto.view;

import com.richardbrenkus.hospitalshiftscheduler.entity.StoredUserSnapshot;
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
