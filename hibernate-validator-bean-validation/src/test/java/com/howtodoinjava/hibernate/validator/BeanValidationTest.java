package com.howtodoinjava.hibernate.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.el.ExpressionFactory;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.executable.ExecutableValidator;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.Test;

class BeanValidationTest {

  private final Validator validator = Validators.validator();

  private Attendee lokesh() {
    return new Attendee("Lokesh", "lokesh@example.com", 37, TicketType.STANDARD);
  }

  private Attendee invalid() {
    return new Attendee("L", "lokesh.example.com", 16, null);
  }

  @Test
  void validAttendeeHasNoViolations() {
    assertTrue(validator.validate(lokesh()).isEmpty());
  }

  @Test
  void invalidAttendeeReportsEveryViolation() {
    assertEquals(List.of(
        "age: must be greater than or equal to 18 (value: 16)",
        "email: must be a well-formed email address (value: lokesh.example.com)",
        "name: name must have 2 to 40 characters (value: L)",
        "ticketType: must not be null (value: null)"),
        Validators.describe(validator.validate(invalid())));
  }

  @Test
  void violationExposesPathMessageTemplateAndValue() {
    ConstraintViolation<Attendee> v = validator.validateProperty(invalid(), "email").iterator().next();
    assertEquals("email", v.getPropertyPath().toString());
    assertEquals("must be a well-formed email address", v.getMessage());
    assertEquals("{jakarta.validation.constraints.Email.message}", v.getMessageTemplate());
    assertEquals("lokesh.example.com", v.getInvalidValue());
    assertEquals(Attendee.class, v.getRootBeanClass());
    assertEquals(Email.class, v.getConstraintDescriptor().getAnnotation().annotationType());
  }

  @Test
  void validatePropertyChecksOnlyThatProperty() {
    assertEquals(1, validator.validateProperty(invalid(), "email").size());
    assertEquals(1, validator.validateValue(Attendee.class, "age", 16).size());
    assertEquals(0, validator.validateValue(Attendee.class, "age", 18).size());
  }

  private boolean valid(String property, String value) {
    return validator.validateValue(BadgeText.class, property, value).isEmpty();
  }

  @Test
  void notNullNotEmptyNotBlankGrid() {
    // null
    assertFalse(valid("notNull", null));
    assertFalse(valid("notEmpty", null));
    assertFalse(valid("notBlank", null));
    // ""
    assertTrue(valid("notNull", ""));
    assertFalse(valid("notEmpty", ""));
    assertFalse(valid("notBlank", ""));
    // " "
    assertTrue(valid("notNull", " "));
    assertTrue(valid("notEmpty", " "));
    assertFalse(valid("notBlank", " "));
    // "Lokesh"
    assertTrue(valid("notNull", "Lokesh"));
    assertTrue(valid("notEmpty", "Lokesh"));
    assertTrue(valid("notBlank", "Lokesh"));
  }

  @Test
  void notEmptyAlsoChecksCollections() {
    record Agenda(@jakarta.validation.constraints.NotEmpty List<String> talks) {
    }
    assertEquals("must not be empty",
        validator.validate(new Agenda(List.of())).iterator().next().getMessage());
  }

  @Test
  void validCascadesIntoTheAddress() {
    Attendee anna = new Attendee("Anna", "anna@example.com", 29, TicketType.VIP);
    anna.setAddress(new Address(" ", "4110"));
    assertEquals(List.of(
        "address.city: must not be blank (value:  )",
        "address.zip: must have 5 digits (value: 4110)"),
        Validators.describe(validator.validate(anna)));
  }

  @Test
  void nullAddressIsNotValidated() {
    Attendee anna = new Attendee("Anna", "anna@example.com", 29, TicketType.VIP);
    assertTrue(validator.validate(anna).isEmpty());
  }

  @Test
  void nestedObjectWithoutValidIsNotChecked() {
    record Badge(Address address) {
    }
    record CheckedBadge(@jakarta.validation.Valid Address address) {
    }
    assertTrue(validator.validate(new Badge(new Address(" ", "4110"))).isEmpty());
    assertEquals(2, validator.validate(new CheckedBadge(new Address(" ", "4110"))).size());
  }

  @Test
  void containerElementConstraintChecksEachEmail() {
    Attendee anna = new Attendee("Anna", "anna@example.com", 29, TicketType.VIP);
    anna.getGuestEmails().add("ravi@example.com");
    anna.getGuestEmails().add("meera-at-example");
    Set<ConstraintViolation<Attendee>> violations = validator.validate(anna);
    assertEquals(List.of("guestEmails[1].<list element>: must be a well-formed email address (value: meera-at-example)"),
        Validators.describe(violations));
    Iterator<Path.Node> nodes = violations.iterator().next().getPropertyPath().iterator();
    assertEquals("guestEmails", nodes.next().getName());
    Path.Node element = nodes.next();
    assertEquals(ElementKind.CONTAINER_ELEMENT, element.getKind());
    assertEquals(1, element.getIndex());
  }

  @Test
  void checkoutGroupRunsOnlyWhenRequested() {
    Attendee attendee = lokesh();
    assertTrue(validator.validate(attendee).isEmpty());
    assertEquals(List.of("termsAccepted: terms must be accepted before payment (value: false)"),
        Validators.describe(validator.validate(attendee, Checkout.class)));
    attendee.setTermsAccepted(true);
    assertTrue(validator.validate(attendee, Checkout.class).isEmpty());
  }

  @Test
  void checkoutGroupSkipsDefaultConstraints() {
    // The invalid attendee breaks four Default rules, but Checkout checks only termsAccepted
    assertEquals(1, validator.validate(invalid(), Checkout.class).size());
  }

  @Test
  void groupSequenceStopsAfterTheFirstFailingGroup() {
    Set<ConstraintViolation<Attendee>> violations = validator.validate(invalid(), RegistrationChecks.class);
    assertEquals(4, violations.size());
    assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("termsAccepted")));

    assertEquals(List.of("termsAccepted: terms must be accepted before payment (value: false)"),
        Validators.describe(validator.validate(lokesh(), RegistrationChecks.class)));
  }

  @Test
  void customClassLevelConstraint() {
    Attendee ravi = new Attendee("Ravi", "ravi@example.com", 30, TicketType.STUDENT);
    Set<ConstraintViolation<Attendee>> violations = validator.validate(ravi);
    assertEquals(1, violations.size());
    ConstraintViolation<Attendee> v = violations.iterator().next();
    assertEquals("age", v.getPropertyPath().toString());
    assertEquals("student tickets are for attendees aged 25 or younger", v.getMessage());
    assertEquals("{attendee.student.age}", v.getMessageTemplate());
    assertSame(ravi, v.getInvalidValue());   // class-level: the whole object

    assertTrue(validator.validate(new Attendee("Ravi", "ravi@example.com", 22, TicketType.STUDENT)).isEmpty());
    assertTrue(validator.validate(new Attendee("Ravi", "ravi@example.com", 30, TicketType.VIP)).isEmpty());
  }

  @Test
  void recordComponentsAreValidated() {
    assertEquals(List.of(
        "minutes: must be less than or equal to 60 (value: 90)",
        "title: must not be blank (value: )"),
        Validators.describe(validator.validate(new Talk("", "Lokesh", 90))));
  }

  @Test
  void recordConstructorDoesNotValidateByItself() {
    // Creating an invalid record works; only a Validator call reports the problem
    Talk talk = new Talk("", "Lokesh", 90);
    assertEquals(90, talk.minutes());
  }

  @Test
  void constructorParametersAreValidated() throws Exception {
    ExecutableValidator executables = validator.forExecutables();
    var constructor = Talk.class.getDeclaredConstructor(String.class, String.class, int.class);
    assertEquals(List.of(
        "Talk.minutes: must be greater than or equal to 15 (value: 5)",
        "Talk.speaker: must not be blank (value:  )"),
        Validators.describe(executables.validateConstructorParameters(constructor,
            new Object[] {"Hibernate Tips", " ", 5})));
  }

  @Test
  void methodParametersAndReturnValueAreValidated() throws Exception {
    ExecutableValidator executables = validator.forExecutables();
    RegistrationService service = new RegistrationService();
    var register = RegistrationService.class.getMethod("register", String.class, String.class, int.class);
    assertEquals(List.of(
        "register.age: must be greater than or equal to 18 (value: 17)",
        "register.email: must be a well-formed email address (value: lokesh)"),
        Validators.describe(executables.validateParameters(service, register,
            new Object[] {"Lokesh", "lokesh", 17})));

    var find = RegistrationService.class.getMethod("findByEmail", String.class);
    assertEquals(List.of("findByEmail.<return value>: must not be null (value: null)"),
        Validators.describe(executables.validateReturnValue(service, find,
            service.findByEmail("nobody@example.com"))));
  }

  @Test
  void plainMethodCallIsNotValidated() {
    // Without a framework that intercepts the call (Spring, CDI), nothing checks the parameters
    Attendee attendee = new RegistrationService().register("Lokesh", "lokesh", 17);
    assertEquals(17, attendee.getAge());
  }

  @Test
  void failFastStopsAtFirstViolation() {
    assertEquals(4, validator.validate(invalid()).size());
    assertEquals(1, Validators.failFastValidator().validate(invalid()).size());
  }

  @Test
  void failFastViaProperty() {
    Validator v = Validation.byProvider(HibernateValidator.class)
        .configure()
        .addProperty("hibernate.validator.fail_fast", "true")
        .buildValidatorFactory()
        .getValidator();
    assertEquals(1, v.validate(invalid()).size());
  }

  @Test
  void hibernateSpecificConstraints() {
    record Extras(
        @org.hibernate.validator.constraints.Length(max = 5) String code,
        @org.hibernate.validator.constraints.Range(min = 1, max = 3) int days,
        @org.hibernate.validator.constraints.URL String website,
        @org.hibernate.validator.constraints.UniqueElements List<String> tags) {
    }
    assertEquals(List.of(
        "code: length must be between 0 and 5 (value: abcdefg)",
        "days: must be between 1 and 3 (value: 4)",
        "tags: must only contain unique elements (value: [java, java])",
        "website: must be a valid URL (value: example)"),
        Validators.describe(validator.validate(new Extras("abcdefg", 4, "example", List.of("java", "java")))));
  }

  @Test
  void messagesFollowTheLocale() {
    Validator german = Validation.byProvider(HibernateValidator.class)
        .configure()
        .defaultLocale(java.util.Locale.GERMAN)
        .buildValidatorFactory()
        .getValidator();
    assertEquals("darf nicht null sein",
        german.validateValue(Attendee.class, "ticketType", null).iterator().next().getMessage());
  }

  @Test
  void ownKeysComeFromTheLocalizedBundle() {
    Validator german = Validation.byProvider(HibernateValidator.class)
        .configure()
        .defaultLocale(java.util.Locale.GERMAN)
        .buildValidatorFactory()
        .getValidator();
    assertEquals("Name muss 2 bis 40 Zeichen haben",
        german.validateValue(Attendee.class, "name", "L").iterator().next().getMessage());
  }

  @Test
  void nullIsValidForConstraintsOtherThanNotNull() {
    assertTrue(validator.validateValue(Address.class, "zip", null).isEmpty());
    record Extras(@jakarta.validation.constraints.Email String email,
                  @jakarta.validation.constraints.Size(min = 2) String code,
                  @jakarta.validation.constraints.Min(1) Integer count) {
    }
    assertTrue(validator.validate(new Extras(null, null, null)).isEmpty());
  }

  @Test
  void hibernateValidatesOnInsertAndRollsBack() {
    try (EntityManagerFactory emf = Database.create(false)) {
      emf.runInTransaction(em -> em.persist(lokesh()));
      List<String> steps = new ArrayList<>();
      ConstraintViolationException e = assertThrows(ConstraintViolationException.class, () ->
          emf.runInTransaction(em -> {
            em.persist(new Attendee("Lokesh", "not-an-email", 37, TicketType.VIP));
            steps.add("persist returned");
            em.flush();
            steps.add("flush returned");
          }));
      assertEquals(List.of("persist returned"), steps);   // sequence id: the INSERT runs at flush
      assertTrue(e.getMessage().startsWith("Validation failed for classes "
          + "[com.howtodoinjava.hibernate.validator.Attendee] during persist time for groups "
          + "[jakarta.validation.groups.Default, ]"), e.getMessage());
      assertEquals(List.of("email: must be a well-formed email address (value: not-an-email)"),
          Validators.describe(e.getConstraintViolations()));
      assertEquals(1, Database.count(emf));
    }
  }

  @Test
  void hibernateValidatesOnUpdate() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Long id = emf.callInTransaction(em -> {
        Attendee a = lokesh();
        em.persist(a);
        return a.getId();
      });
      // No explicit flush: the check runs at commit and the exception arrives wrapped
      RollbackException e = assertThrows(RollbackException.class, () ->
          emf.runInTransaction(em -> em.find(Attendee.class, id).setEmail("lokesh")));
      assertTrue(e.getCause() instanceof ConstraintViolationException);
      assertTrue(e.getMessage().startsWith("Error while committing the transaction [Validation failed for classes "
          + "[com.howtodoinjava.hibernate.validator.Attendee] during update time"), e.getMessage());
      assertEquals("lokesh@example.com",
          emf.callInTransaction(em -> em.find(Attendee.class, id).getEmail()));
    }
  }

  @Test
  void violationAtCommitIsWrappedInRollbackException() {
    try (EntityManagerFactory emf = Database.create(false)) {
      RollbackException e = assertThrows(RollbackException.class, () ->
          emf.runInTransaction(em -> em.persist(new Attendee("Lokesh", "not-an-email", 37, TicketType.VIP))));
      ConstraintViolationException cause = (ConstraintViolationException) e.getCause();
      assertEquals(1, cause.getConstraintViolations().size());
      assertEquals(0, Database.count(emf));
    }
  }

  @Test
  void identityIdValidatesInsidePersist() {
    try (EntityManagerFactory emf = new org.hibernate.jpa.HibernatePersistenceConfiguration("sponsors")
        .managedClasses(Sponsor.class)
        .jdbcUrl("jdbc:h2:mem:sponsors;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(org.hibernate.tool.schema.Action.CREATE_DROP)
        .createEntityManagerFactory()) {
      List<String> steps = new ArrayList<>();
      ConstraintViolationException e = assertThrows(ConstraintViolationException.class, () ->
          emf.runInTransaction(em -> {
            try {
              em.persist(new Sponsor(" "));
              steps.add("persist returned");
            } catch (ConstraintViolationException inPersist) {
              steps.add("thrown by persist");
              throw inPersist;
            }
          }));
      assertEquals(List.of("thrown by persist"), steps);
      assertTrue(e.getMessage().contains("Sponsor] during persist time"), e.getMessage());
    }
  }

  @Test
  void validationModeNoneTurnsTheCheckOff() {
    try (EntityManagerFactory emf = new org.hibernate.jpa.HibernatePersistenceConfiguration("no-validation")
        .managedClasses(Attendee.class, Address.class)
        .jdbcUrl("jdbc:h2:mem:novalidation;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(org.hibernate.tool.schema.Action.CREATE_DROP)
        .validationMode(jakarta.persistence.ValidationMode.NONE)
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(new Attendee("Lokesh", "not-an-email", 37, TicketType.VIP)));
      assertEquals(1L, (long) emf.callInTransaction(em ->
          em.createQuery("select count(*) from Attendee", Long.class).getSingleResult()));
    }
  }

  @Test
  void checkoutGroupIsNotCheckedOnPersist() {
    try (EntityManagerFactory emf = Database.create(false)) {
      emf.runInTransaction(em -> em.persist(lokesh()));   // termsAccepted = false is accepted
      assertEquals(1, Database.count(emf));
    }
  }

  @Test
  void constraintsShapeTheGeneratedSchema() {
    try (EntityManagerFactory emf = Database.create(false)) {
      List<Object[]> rows = emf.callInTransaction(em -> em.createNativeQuery(
          "select column_name, is_nullable, character_maximum_length from information_schema.columns "
              + "where table_name = 'ATTENDEE' and column_name in ('NAME','EMAIL','CITY') order by column_name",
          Object[].class).getResultList());
      assertEquals("CITY YES 255", row(rows.get(0)));
      assertEquals("EMAIL NO 255", row(rows.get(1)));
      assertEquals("NAME NO 40", row(rows.get(2)));
      String check = emf.callInTransaction(em -> (String) em.createNativeQuery(
          "select check_clause from information_schema.check_constraints where check_clause like '%AGE%'")
          .getSingleResult());
      assertTrue(check.contains("\"AGE\" >= 18") && check.contains("\"AGE\" <= 120"), check);
    }
  }

  private static String row(Object[] r) {
    return r[0] + " " + r[1] + " " + r[2];
  }

  @Test
  void bootstrapFailsWithoutAnExpressionLanguageImplementation() throws Exception {
    // Class path with Hibernate Validator but without Expressly and the EL API
    URL[] jars = {
        location(Validation.class), location(HibernateValidator.class),
        location(org.jboss.logging.Logger.class), location(com.fasterxml.classmate.TypeResolver.class)};
    assertFalse(List.of(jars).contains(location(ExpressionFactory.class)));
    ClassLoader previous = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader loader = new URLClassLoader(jars, ClassLoader.getPlatformClassLoader())) {
      Thread.currentThread().setContextClassLoader(loader);
      Class<?> validation = loader.loadClass("jakarta.validation.Validation");
      InvocationTargetException e = assertThrows(InvocationTargetException.class, () ->
          validation.getMethod("buildDefaultValidatorFactory").invoke(null));
      Throwable cause = e.getCause();
      System.out.println("[no EL] " + cause.getClass().getName() + ": " + cause.getMessage());
      for (Throwable t = cause.getCause(); t != null; t = t.getCause()) {
        System.out.println("[no EL] Caused by: " + t.getClass().getName() + ": " + t.getMessage());
      }
      assertEquals("jakarta.validation.ValidationException", cause.getClass().getName());
      assertEquals("HV000183: Unable to initialize 'jakarta.el.ExpressionFactory'. Check that you have the EL "
          + "dependencies on the classpath, or use ParameterMessageInterpolator instead", cause.getMessage());
      assertEquals("java.lang.NoClassDefFoundError", cause.getCause().getClass().getName());
    } finally {
      Thread.currentThread().setContextClassLoader(previous);
    }
  }

  private static URL location(Class<?> type) {
    return type.getProtectionDomain().getCodeSource().getLocation();
  }
}
