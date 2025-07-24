package com.beaver.core.auth.validation;

import com.beaver.core.auth.dto.IAuthRequest;
import com.beaver.core.user.UserService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, IAuthRequest> {

    private final UserService userService;

    @Override
    public void initialize(UniqueEmail constraintAnnotation) {}

    @Override
    public boolean isValid(IAuthRequest value, ConstraintValidatorContext context) {
        boolean emailExists = userService.findByEmail(value.email()).isPresent();

        if (emailExists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Email already exists")
                    .addPropertyNode("email")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
