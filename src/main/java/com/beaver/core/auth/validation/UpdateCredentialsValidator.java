package com.beaver.core.auth.validation;

import com.beaver.core.auth.dto.UpdateCredentials;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateCredentialsValidator implements ConstraintValidator<ValidUpdateCredentials, UpdateCredentials> {

    @Override
    public void initialize(ValidUpdateCredentials constraintAnnotation) {}

    @Override
    public boolean isValid(UpdateCredentials updateCredentials, ConstraintValidatorContext context) {
        boolean isValid = true;

        return isValid;
    }
}
