package com.beaver.core.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UpdateCredentialsValidator.class)
public @interface ValidUpdateCredentials {
    String message() default "Invalid credentials update request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
