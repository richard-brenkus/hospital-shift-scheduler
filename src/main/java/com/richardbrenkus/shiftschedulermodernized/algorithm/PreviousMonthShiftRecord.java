package com.richardbrenkus.shiftschedulermodernized.algorithm;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviousMonthShiftRecord {

    private Integer backwardIndex;
    private String dateStringDB;
    private Long dateIdDB;

    @Builder.Default
    private Map<Integer, String> usernameByShiftType = new HashMap<>();

    public String getUsernameForShiftType(int shiftType) {
        return usernameByShiftType.getOrDefault(shiftType, "");
    }

    public void setUsernameForShiftType(int shiftType, String username) {
        usernameByShiftType.put(shiftType, username == null ? "" : username);
    }

    public boolean containsUsername(String username) {
        if (username == null) {
            return false;
        }

        return usernameByShiftType.values()
                .stream()
                .anyMatch(username::equals);
    }
}
