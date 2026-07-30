package com.richardbrenkus.hospitalshiftscheduler.dto.view;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserValidationResult {

    private boolean isValid;

    private List<ValidationError> fieldErrors;
    private List<ValidationError> globalErrors;

    public void addFieldError(ValidationError validationError) {
        this.fieldErrors.add(validationError);
    }

    public void addGlobalError(ValidationError validationError) {
        this.globalErrors.add(validationError);
    }

    public static UserValidationResult valid() {
        return new UserValidationResult(true, List.of(), List.of());
    }

}


