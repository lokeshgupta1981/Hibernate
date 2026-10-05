package com.howtodoinjava.hibernate.unexpectedtype;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom constraint for the Recipient type, checked by RecipientValidator.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RecipientValidator.class)
public @interface ValidRecipient {

  String message() default "must have a name and an email address";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
