package com.richardbrenkus.hospitalshiftscheduler.dto.form;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.ValidationConstants;
import jakarta.persistence.Column;
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
public class UserRegisterForm {

    private Long id;

    @NotBlank(message = "{user.name.NotBlank}")
    @Size(min = ValidationConstants.NAME_MIN_LENGTH, max = ValidationConstants.NAME_MAX_LENGTH, message = "{user.name.size}")
    private String name;

    @NotBlank(message = "{user.username.NotBlank}")
    @Size(min = ValidationConstants.NAME_MIN_LENGTH, max = ValidationConstants.NAME_MAX_LENGTH, message = "{user.username.size}")
    private String username;

    @NotBlank(message = "{user.email.NotBlank}")
    @Email(message = "{user.email.invalid}")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "{user.password.NotBlank}")
    @Size(
            min = ValidationConstants.PASSWORD_MIN_LENGTH,
            max = ValidationConstants.PASSWORD_MAX_LENGTH,
            message = "{user.password.size}"
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "{user.password.weak}"
    )
    private String password;

    private String note;
    private String birthday;
    private String profession;
    private String title;

    private Set<Integer> allowedShiftTypes = new HashSet<>();
}
