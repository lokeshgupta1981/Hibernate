package com.howtodoinjava.hibernate.validatorcdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorCdiTest {

  static SeContainer container;

  final TaxFiling valid = new TaxFiling("lokesh", 2025, "52000.00", "4000.00");
  final TaxFiling invalid = new TaxFiling("robin", 2027, "-10.00", "4000.00");

  TaxFilingService service;
  FilingInspector inspector;

  @BeforeAll
  static void startContainer() {
    System.setProperty("org.jboss.logging.provider", "slf4j");
    container = SeContainerInitializer.newInstance().initialize();
  }

  @AfterAll
  static void stopContainer() {
    container.close();
  }

  @BeforeEach
  void lookUpBeans() {
    service = container.select(TaxFilingService.class).get();
    inspector = container.select(FilingInspector.class).get();
    service.reset();
  }

  // --- 1. Without CDI ---

  @Test
  void plainFactoryDoesNotInjectIntoConstraintValidator() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = factory.getValidator();
      ValidationException e = assertThrows(ValidationException.class, () -> validator.validate(valid));
      assertEquals("HV000028: Unexpected exception during isValid call.", e.getMessage());
      assertInstanceOf(NullPointerException.class, e.getCause());
      assertTrue(e.getCause().getMessage().contains("\"this.registry\" is null"));
    }
  }

  // --- 2. Injected Validator and ValidatorFactory ---

  @Test
  void injectedValidatorValidatesBeans() {
    assertEquals(List.of(), inspector.check(valid));
    assertEquals(List.of(
        "income must be greater than or equal to 0",
        "taxpayerId is not a registered taxpayer",
        "year must be a date in the past or in the present"), inspector.check(invalid));
  }

  @Test
  void defaultAndHibernateValidatorQualifiersShareOneFactory() {
    assertNotNull(inspector.validator());
    assertNotNull(inspector.hibernateValidator());
    assertEquals(3, inspector.hibernateValidator().validate(invalid).size());
    // Hibernate Validator is the default provider, so one bean serves both qualifiers
    assertSame(inspector.validatorFactory().unwrap(HibernateValidatorFactory.class),
        inspector.hibernateValidatorFactory());
  }

  @Test
  void validatorCanBeLookedUpFromContainer() {
    Validator validator = container.select(Validator.class).get();
    Set<ConstraintViolation<TaxFiling>> violations = validator.validate(invalid);
    assertEquals(3, violations.size());
  }

  // --- 3. Dependency injection in a ConstraintValidator ---

  @Test
  void constraintValidatorGetsRegistryInjected() {
    TaxFiling unknown = new TaxFiling("robin", 2025, "100.00", "0.00");
    assertEquals(List.of("taxpayerId is not a registered taxpayer"), inspector.check(unknown));
  }

  // --- 4. Method validation ---

  @Test
  void validArgumentsReachTheMethod() {
    assertEquals("Filed 2025 for lokesh", service.submit(valid));
  }

  @Test
  void invalidArgumentThrowsConstraintViolationException() {
    ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
        () -> service.submit(invalid));
    assertTrue(e.getMessage().startsWith("3 constraint violation(s) occurred during method validation."));
    assertEquals(Set.of(
            "submit.filing.taxpayerId is not a registered taxpayer",
            "submit.filing.year must be a date in the past or in the present",
            "submit.filing.income must be greater than or equal to 0"),
        paths(e));
    assertNull(service.getLastReceipt());   // the method body never ran
    // the interceptor runs on the Weld subclass of the bean
    assertTrue(e.getConstraintViolations().iterator().next().getRootBean()
        .getClass().getName().endsWith("TaxFilingService$Proxy$_$$_WeldSubclass"));
  }

  @Test
  void nullArgumentIsRejected() {
    ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
        () -> service.submit(null));
    assertEquals(Set.of("submit.filing must not be null"), paths(e));
  }

  @Test
  void returnValueIsValidated() {
    assertEquals(new BigDecimal("48000.00"), service.taxableIncome(valid));
    ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
        () -> service.taxableIncome(new TaxFiling("alex", 2025, "3000.00", "4000.00")));
    assertEquals(Set.of("taxableIncome.<return value> must be greater than or equal to 0"), paths(e));
  }

  @Test
  void gettersAreNotValidatedUnlessEnabled() {
    assertNull(service.getLastReceipt());
    ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
        () -> service.getCheckedReceipt());
    assertEquals(Set.of("getCheckedReceipt.<return value> must not be null"), paths(e));
  }

  @Test
  void objectCreatedWithNewIsNotValidated() {
    TaxFilingService plain = new TaxFilingService();
    assertEquals("Filed 2027 for robin", plain.submit(invalid));
  }

  // --- 5. validation.xml ---

  @Test
  void clockProviderFromValidationXmlIsUsed() {
    // the fixed clock says 2026-04-15, so 2026 is present and 2027 is future
    assertEquals(List.of(), inspector.check(new TaxFiling("alex", 2026, "1.00", "0.00")));
    assertEquals(List.of("year must be a date in the past or in the present"),
        inspector.check(new TaxFiling("alex", 2027, "1.00", "0.00")));
  }

  // --- 6. Hibernate ORM ---

  @Test
  void hibernateUsesTheCdiValidatorFactory() {
    try (EntityManagerFactory emf = Database.create("filings-test", inspector.validatorFactory())) {
      emf.runInTransaction(em -> em.persist(new TaxFiling("priya", 2025, "100.00", "0.00")));
      RollbackException e = assertThrows(RollbackException.class,
          () -> emf.runInTransaction(em -> em.persist(new TaxFiling("robin", 2025, "100.00", "0.00"))));
      ConstraintViolationException cause = assertInstanceOf(ConstraintViolationException.class, e.getCause());
      assertEquals(Set.of("taxpayerId is not a registered taxpayer"), paths(cause));
      long rows = emf.callInTransaction(em ->
          em.createQuery("select count(*) from TaxFiling", Long.class).getSingleResult());
      assertEquals(1, rows);
    }
  }

  @Test
  void hibernateOwnFactoryCannotInject() {
    try (EntityManagerFactory emf = Database.create("filings-plain-test", null)) {
      RollbackException e = assertThrows(RollbackException.class,
          () -> emf.runInTransaction(em -> em.persist(new TaxFiling("priya", 2025, "100.00", "0.00"))));
      assertInstanceOf(ValidationException.class, e.getCause());
      assertInstanceOf(NullPointerException.class, e.getCause().getCause());
    }
  }

  // --- 7. Why is my injected Validator null? ---

  @Test
  void staticFieldIsNotInjected() {
    try (SeContainer other = SeContainerInitializer.newInstance()
        .disableDiscovery()
        .addBeanClasses(StaticHolder.class)
        .initialize()) {
      assertNotNull(other.select(StaticHolder.class).get());
      assertNull(StaticHolder.validator);   // no error, the field just stays null
    }
  }

  @Test
  void weldSeNeedsBeansXml() {
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    ClassLoader withoutBeansXml = new ClassLoader(original) {
      @Override
      public java.util.Enumeration<java.net.URL> getResources(String name) throws java.io.IOException {
        return name.equals("META-INF/beans.xml") ? java.util.Collections.emptyEnumeration() : super.getResources(name);
      }
    };
    Thread.currentThread().setContextClassLoader(withoutBeansXml);
    try {
      IllegalStateException e = assertThrows(IllegalStateException.class,
          () -> SeContainerInitializer.newInstance().initialize());
      // a project without beans.xml fails with WELD-ENV-000016 (Missing beans.xml file in META-INF);
      // with the file only hidden from this class loader, Weld reports WELD-ENV-002009 (no bean archives found)
      assertTrue(e.getMessage().startsWith("WELD-ENV-"), e.getMessage());
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  @ApplicationScoped
  static class StaticHolder {
    @Inject
    static Validator validator;
  }

  private static Set<String> paths(ConstraintViolationException e) {
    return e.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() + " " + v.getMessage())
        .collect(java.util.stream.Collectors.toSet());
  }
}
