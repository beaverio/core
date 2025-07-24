package com.beaver.core.auth.validation;

import static java.lang.String.format;
import com.beaver.core.auth.dto.UpdateCredentials;
import com.beaver.core.user.UserService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateCredentialsValidator implements ConstraintValidator<ValidUpdateCredentials, UpdateCredentials> {

    private static final String EMAIL_ALREADY_EXISTS = "Email `%s` already exists";

    private final UserService userService;

    @Override
    public void initialize(ValidUpdateCredentials constraintAnnotation) {}

    @Override
    public boolean isValid(UpdateCredentials credentials, ConstraintValidatorContext context) {
        boolean isValid;

        isValid = isUniqueEmail(credentials.email(), context);

        return isValid;
    }

    private boolean isUniqueEmail(String email, ConstraintValidatorContext context) {
        boolean emailExists = userService.findByEmail(email).isPresent();

        if (emailExists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(format(EMAIL_ALREADY_EXISTS, email))
                    .addPropertyNode("email")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
