package com.richardbrenkus.shiftschedulermodernized.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateForm {

    private Long id;

    @NotNull
    @Size(min = 2, max = 45)
    private String name;

    @NotNull
    @Size(min = 2, max = 45)
    private String username;

    @Email
    private String email;

    private String note;
    private String birthday;
    private String profession;
    private String title;
    private boolean enabled;

    private Set<Integer> allowedShiftTypes = new HashSet<>();
}