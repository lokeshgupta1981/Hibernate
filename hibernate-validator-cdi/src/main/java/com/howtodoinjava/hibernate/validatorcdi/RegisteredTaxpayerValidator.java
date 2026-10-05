package com.howtodoinjava.hibernate.validatorcdi;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RegisteredTaxpayerValidator implements ConstraintValidator<RegisteredTaxpayer, String> {

  @Inject
  TaxpayerRegistry registry;   // injected by CDI, null with Validation.buildDefaultValidatorFactory()

  @Override
  public boolean isValid(String taxpayerId, ConstraintValidatorContext context) {
    if (taxpayerId == null) {
      return true;             // @NotBlank reports null values
    }
    return registry.isRegistered(taxpayerId);
  }
}
