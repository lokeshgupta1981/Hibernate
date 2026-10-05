package com.howtodoinjava.hibernate.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.hibernate.validator.HibernateValidator;

public final class Validators {

  static {
    // The built-in messages are localized; English keeps the output identical on every machine.
    Locale.setDefault(Locale.ENGLISH);
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

  private static final ValidatorFactory FAIL_FAST_FACTORY = Validation.byProvider(HibernateValidator.class)
      .configure()
      .failFast(true)
      .buildValidatorFactory();

  private Validators() {
  }

  public static Validator validator() {
    return FACTORY.getValidator();
  }

  public static Validator failFastValidator() {
    return FAIL_FAST_FACTORY.getValidator();
  }

  /** One line per violation, sorted by path, because the Set has no fixed order. */
  public static List<String> describe(Set<? extends ConstraintViolation<?>> violations) {
    return violations.stream()
        .sorted(Comparator.comparing((ConstraintViolation<?> v) -> v.getPropertyPath().toString())
            .thenComparing(ConstraintViolation::getMessage))
        .map(v -> v.getPropertyPath() + ": " + v.getMessage() + " (value: " + v.getInvalidValue() + ")")
        .toList();
  }
}
