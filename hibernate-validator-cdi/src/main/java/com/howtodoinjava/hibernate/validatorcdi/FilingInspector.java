package com.howtodoinjava.hibernate.validatorcdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidatorFactory;
import org.hibernate.validator.cdi.HibernateValidator;

import java.util.List;

/**
 * Shows the four injection points the Hibernate Validator CDI extension provides.
 */
@ApplicationScoped
public class FilingInspector {

  @Inject
  Validator validator;                        // @Default

  @Inject
  ValidatorFactory validatorFactory;          // @Default

  @Inject
  @HibernateValidator
  Validator hibernateValidator;

  @Inject
  @HibernateValidator
  ValidatorFactory hibernateValidatorFactory;

  public List<String> check(TaxFiling filing) {
    return validator.validate(filing).stream()
        .map(v -> v.getPropertyPath() + " " + v.getMessage())
        .sorted()
        .toList();
  }

  public Validator validator() {
    return validator;
  }

  public ValidatorFactory validatorFactory() {
    return validatorFactory;
  }

  public Validator hibernateValidator() {
    return hibernateValidator;
  }

  public HibernateValidatorFactory hibernateValidatorFactory() {
    return hibernateValidatorFactory.unwrap(HibernateValidatorFactory.class);
  }
}
