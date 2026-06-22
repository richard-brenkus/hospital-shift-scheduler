package com.richardbrenkus.shiftschedulermodernized.dto.form;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ValidationConstants;
import jakarta.validation.constraints.*;
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
public class UserForm {

    private Long id;

    @NotNull
    @Size(min = ValidationConstants.NAME_MIN_LENGTH, max = ValidationConstants.NAME_MAX_LENGTH)
    private String name;

    @NotNull
    @Size(min = ValidationConstants.NAME_MIN_LENGTH, max = ValidationConstants.NAME_MAX_LENGTH)
    private String username;

    @Email
    private String email;

    @NotBlank
    @Size(
            min = ValidationConstants.PASSWORD_MIN_LENGTH,
            max = ValidationConstants.PASSWORD_MAX_LENGTH,
            message = "{validation.password.size}"
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "{validation.password.weak}"
    )
    private String password;

    private String note;
    private String birthday;
    private String profession;
    private String title;

    private Set<Integer> allowedShiftTypes = new HashSet<>();
}
