package com.howtodoinjava.hibernate.validator;

import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class BeanValidationDemo {

  public static void main(String[] args) throws Exception {
    Validator validator = Validators.validator();

    step("1. Validate a valid attendee");
    Attendee lokesh = new Attendee("Lokesh", "lokesh@example.com", 37, TicketType.STANDARD);
    System.out.println("violations: " + validator.validate(lokesh).size());

    step("2. Validate an invalid attendee");
    Attendee invalid = new Attendee("L", "lokesh.example.com", 16, null);
    Set<ConstraintViolation<Attendee>> violations = validator.validate(invalid);
    System.out.println("violations: " + violations.size());
    print(violations);

    step("3. Read one violation");
    ConstraintViolation<Attendee> v = validator.validateProperty(invalid, "email").iterator().next();
    System.out.println("getPropertyPath()    = " + v.getPropertyPath());
    System.out.println("getMessage()         = " + v.getMessage());
    System.out.println("getMessageTemplate() = " + v.getMessageTemplate());
    System.out.println("getInvalidValue()    = " + v.getInvalidValue());
    System.out.println("getRootBeanClass()   = " + v.getRootBeanClass().getSimpleName());
    System.out.println("constraint           = "
        + v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName());

    step("4. @NotNull vs @NotEmpty vs @NotBlank");
    for (String value : new String[] {null, "", " ", "Lokesh"}) {
      System.out.printf("%-10s notNull=%s notEmpty=%s notBlank=%s%n",
          value == null ? "null" : "\"" + value + "\"",
          ok(validator, "notNull", value), ok(validator, "notEmpty", value), ok(validator, "notBlank", value));
    }

    step("5. Cascaded validation of the address with @Valid");
    Attendee withAddress = new Attendee("Anna", "anna@example.com", 29, TicketType.VIP);
    withAddress.setAddress(new Address(" ", "4110"));
    print(validator.validate(withAddress));

    step("6. Container element constraints: List<@Email String>");
    Attendee withGuests = new Attendee("Anna", "anna@example.com", 29, TicketType.VIP);
    withGuests.getGuestEmails().add("ravi@example.com");
    withGuests.getGuestEmails().add("meera-at-example");
    print(validator.validate(withGuests));

    step("7. Groups: Checkout rules run only when we ask for them");
    System.out.println("Default group:  " + Validators.describe(validator.validate(lokesh)));
    System.out.println("Checkout group: " + Validators.describe(validator.validate(lokesh, Checkout.class)));
    lokesh.setTermsAccepted(true);
    System.out.println("Checkout group after accepting terms: "
        + Validators.describe(validator.validate(lokesh, Checkout.class)));
    lokesh.setTermsAccepted(false);

    step("8. Group sequence: Checkout runs only if Default passes");
    System.out.println("invalid attendee: " + validator.validate(invalid, RegistrationChecks.class).size()
        + " violations, terms checked: "
        + validator.validate(invalid, RegistrationChecks.class).stream()
            .anyMatch(x -> x.getPropertyPath().toString().equals("termsAccepted")));
    print(validator.validate(lokesh, RegistrationChecks.class));

    step("9. Custom class-level constraint @StudentTicket");
    print(validator.validate(new Attendee("Ravi", "ravi@example.com", 30, TicketType.STUDENT)));
    System.out.println("age 22: " + validator.validate(new Attendee("Ravi", "ravi@example.com", 22, TicketType.STUDENT)).size());

    step("10. Validate a record");
    print(validator.validate(new Talk("", "Lokesh", 90)));

    step("11. Constructor and method validation with ExecutableValidator");
    ExecutableValidator executables = validator.forExecutables();
    Constructor<Talk> constructor = Talk.class.getDeclaredConstructor(String.class, String.class, int.class);
    print(executables.validateConstructorParameters(constructor, new Object[] {"Hibernate Tips", " ", 5}));

    RegistrationService service = new RegistrationService();
    Method register = RegistrationService.class.getMethod("register", String.class, String.class, int.class);
    print(executables.validateParameters(service, register, new Object[] {"Lokesh", "lokesh", 17}));

    Method find = RegistrationService.class.getMethod("findByEmail", String.class);
    Object result = service.findByEmail("nobody@example.com");
    print(executables.validateReturnValue(service, find, result));

    step("12. Fail-fast mode");
    Set<ConstraintViolation<Attendee>> first = Validators.failFastValidator().validate(invalid);
    System.out.println("violations: " + first.size());

    step("13. Hibernate ORM validates before insert");
    try (EntityManagerFactory emf = Database.create(true)) {
      emf.runInTransaction(em -> em.persist(new Attendee("Lokesh", "lokesh@example.com", 37, TicketType.VIP)));
      System.out.println("rows: " + Database.count(emf));
      try {
        emf.runInTransaction(em -> {
          em.persist(new Attendee("Lokesh", "not-an-email", 37, TicketType.VIP));
          System.out.println("persist() returned");
          em.flush();
          System.out.println("flush() returned");
        });
      } catch (ConstraintViolationException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
        print(e.getConstraintViolations());
      }
      System.out.println("rows: " + Database.count(emf));
    }
  }

  private static String ok(Validator validator, String property, String value) {
    return validator.validateValue(BadgeText.class, property, value).isEmpty() ? "valid" : "INVALID";
  }

  private static void print(Set<? extends ConstraintViolation<?>> violations) {
    List<String> lines = Validators.describe(violations);
    if (lines.isEmpty()) {
      System.out.println("(no violations)");
    }
    lines.forEach(System.out::println);
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }
}
