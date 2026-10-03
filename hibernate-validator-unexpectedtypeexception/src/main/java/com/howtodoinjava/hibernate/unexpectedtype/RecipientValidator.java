package com.howtodoinjava.hibernate.unexpectedtype;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Tells Hibernate Validator how to check a Recipient.
 * The second type parameter is the type this validator accepts.
 */
public class RecipientValidator implements ConstraintValidator<ValidRecipient, Recipient> {

  @Override
  public boolean isValid(Recipient recipient, ConstraintValidatorContext context) {
    if (recipient == null) {
      return true; // leave null checks to @NotNull
    }
    return recipient.name() != null && !recipient.name().isBlank()
        && recipient.email() != null && recipient.email().contains("@");
  }
}
