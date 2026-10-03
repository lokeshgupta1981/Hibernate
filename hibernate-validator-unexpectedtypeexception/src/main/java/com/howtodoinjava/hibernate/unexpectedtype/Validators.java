package com.howtodoinjava.hibernate.unexpectedtype;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds the ValidatorFactory and formats violations as "property: message".
 */
public final class Validators {

  private Validators() {
  }

  public static ValidatorFactory create() {
    return Validation.buildDefaultValidatorFactory();
  }

  public static Set<String> describe(Set<? extends ConstraintViolation<?>> violations) {
    Set<String> result = new TreeSet<>();
    for (ConstraintViolation<?> v : violations) {
      result.add(v.getPropertyPath() + ": " + v.getMessage());
    }
    return result;
  }

  public static Set<String> check(Validator validator, Object bean) {
    return describe(validator.validate(bean));
  }
}
