package com.howtodoinjava.hibernate.validatorcdi;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The taxpayer id must exist in the TaxpayerRegistry. The check needs a CDI bean,
 * so the validator gets the registry with @Inject.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RegisteredTaxpayerValidator.class)
public @interface RegisteredTaxpayer {

  String message() default "is not a registered taxpayer";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
