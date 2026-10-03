package com.howtodoinjava.hibernate.validatorcdi;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class ValidatorCdiDemo {

  public static void main(String[] args) {
    System.setProperty("org.jboss.logging.provider", "slf4j");

    TaxFiling valid = new TaxFiling("lokesh", 2025, "52000.00", "4000.00");
    TaxFiling invalid = new TaxFiling("robin", 2027, "-10.00", "4000.00");

    System.out.println("== 1. Without CDI: Validation.buildDefaultValidatorFactory()");
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = factory.getValidator();
      validator.validate(valid);
    } catch (ValidationException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
      System.out.println("Caused by: " + e.getCause());
    }

    System.out.println("\n== 2. With CDI: start Weld SE");
    try (SeContainer container = SeContainerInitializer.newInstance().initialize()) {

      FilingInspector inspector = container.select(FilingInspector.class).get();
      System.out.println("valid   -> " + inspector.check(valid));
      System.out.println("invalid -> " + inspector.check(invalid));
      System.out.println("@HibernateValidator factory -> " + inspector.hibernateValidatorFactory().getClass().getName());

      System.out.println("\n== 3. Method validation on a CDI bean");
      TaxFilingService service = container.select(TaxFilingService.class).get();
      System.out.println(service.submit(valid));
      try {
        service.submit(invalid);
      } catch (ConstraintViolationException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      System.out.println("\n== 4. Return value validation");
      System.out.println("taxableIncome -> " + service.taxableIncome(valid));
      try {
        service.taxableIncome(new TaxFiling("alex", 2025, "3000.00", "4000.00"));
      } catch (ConstraintViolationException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      System.out.println("\n== 5. Getters");
      service.reset();
      System.out.println("getLastReceipt() -> " + service.getLastReceipt());
      try {
        service.getCheckedReceipt();
      } catch (ConstraintViolationException e) {
        System.out.println("getCheckedReceipt() -> " + e.getConstraintViolations().iterator().next().getPropertyPath()
            + " " + e.getConstraintViolations().iterator().next().getMessage());
      }

      System.out.println("\n== 6. Hibernate ORM with the CDI ValidatorFactory");
      try (EntityManagerFactory emf = Database.create("filings", inspector.validatorFactory())) {
        emf.runInTransaction(em -> em.persist(valid));
        try {
          emf.runInTransaction(em -> em.persist(new TaxFiling("robin", 2025, "100.00", "0.00")));
        } catch (RuntimeException e) {
          System.out.println(e.getClass().getName());
          System.out.println("Caused by: " + e.getCause().getClass().getName());
          ((ConstraintViolationException) e.getCause()).getConstraintViolations()
              .forEach(v -> System.out.println("  " + v.getPropertyPath() + " " + v.getMessage()));
        }
      }
    }

    System.out.println("\n== 7. Hibernate ORM with its own ValidatorFactory");
    try (EntityManagerFactory emf = Database.create("filings-plain", null)) {
      emf.runInTransaction(em -> em.persist(new TaxFiling("priya", 2025, "100.00", "0.00")));
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
      Throwable c = e.getCause();
      while (c != null) {
        System.out.println("Caused by: " + c);
        c = c.getCause();
      }
    }
  }
}
