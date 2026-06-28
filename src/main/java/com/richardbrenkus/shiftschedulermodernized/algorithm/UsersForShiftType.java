package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.entity.User;
import lombok.Builder;

import java.util.List;

@Builder
public record UsersForShiftType(
        List<User> specificDateUsers,
        List<User> anyDateUsers
) {
}
