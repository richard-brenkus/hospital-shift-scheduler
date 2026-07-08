package com.richardbrenkus.shiftschedulermodernized.dto.form;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeForm {

    @NotBlank
    @Size(
            min = ValidationConstants.PASSWORD_MIN_LENGTH,
            max = ValidationConstants.PASSWORD_MAX_LENGTH,
            message = "{user.password.size}"
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "{user.password.weak}"
    )
    private String newPassword;

    @NotBlank
    private String confirmedPassword;

    public boolean passwordsMatch() {
        return newPassword != null && newPassword.equals(confirmedPassword);
    }
}
