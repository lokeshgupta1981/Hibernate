package com.howtodoinjava.hibernate.unexpectedtype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.howtodoinjava.hibernate.unexpectedtype.WrongOrders.*;
import jakarta.validation.ConstraintDeclarationException;
import jakarta.validation.UnexpectedTypeException;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnexpectedTypeTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validators.create();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  private static String errorOf(Object bean) {
    UnexpectedTypeException e = assertThrows(UnexpectedTypeException.class,
        () -> validator.validate(bean));
    return e.getMessage();
  }

  private static String hv30(String constraint, String type, String property) {
    return "HV000030: No validator could be found for constraint 'jakarta.validation.constraints."
        + constraint + "' validating type '" + type + "'. Check configuration for '" + property + "'";
  }

  private static Set<String> check(Object bean) {
    return Validators.check(validator, bean);
  }

  // ---------- Causes ----------

  @Test
  void notBlankOnInteger() {
    assertEquals(hv30("NotBlank", "java.lang.Integer", "quantity"),
        errorOf(new NotBlankQuantity(2)));
  }

  @Test
  void notBlankOnIntegerThrowsEvenWhenValueIsNull() {
    assertEquals(hv30("NotBlank", "java.lang.Integer", "quantity"),
        errorOf(new NotBlankQuantity(null)));
  }

  @Test
  void notEmptyOnLocalDate() {
    assertEquals(hv30("NotEmpty", "java.time.LocalDate", "deliveryDate"),
        errorOf(new NotEmptyDeliveryDate(LocalDate.of(2026, 12, 24))));
  }

  @Test
  void sizeOnInteger() {
    assertEquals(hv30("Size", "java.lang.Integer", "quantity"), errorOf(new SizeQuantity(2)));
  }

  @Test
  void emailOnOwnType() {
    assertEquals(
        hv30("Email", "com.howtodoinjava.hibernate.unexpectedtype.Recipient", "recipient"),
        errorOf(new EmailOnRecipient(new Recipient("Lokesh", "lokesh@example.com"))));
  }

  @Test
  void pastOnString() {
    assertEquals(hv30("Past", "java.lang.String", "deliveryDate"),
        errorOf(new PastOnString("2026-01-15")));
  }

  @Test
  void emailOnListInsteadOfElements() {
    assertEquals(hv30("Email", "java.util.List<java.lang.String>", "ccEmails"),
        errorOf(new EmailOnList(List.of("anna@example.com"))));
  }

  @Test
  void notBlankOnOptional() {
    assertEquals(hv30("NotBlank", "java.util.Optional<java.lang.String>", "message"),
        errorOf(new NotBlankOnOptional(Optional.of("Hi"))));
  }

  @Test
  void exceptionHierarchy() {
    UnexpectedTypeException e = assertThrows(UnexpectedTypeException.class,
        () -> validator.validate(new SizeQuantity(2)));
    assertInstanceOf(ConstraintDeclarationException.class, e);
    assertInstanceOf(ValidationException.class, e);
    assertInstanceOf(RuntimeException.class, e);
  }

  @Test
  void validatorFactoryStartsFineWithWrongMappings() {
    // No error at bootstrap or getValidator(); only validate() throws
    try (ValidatorFactory f = Validators.create()) {
      Validator v = f.getValidator();
      assertThrows(UnexpectedTypeException.class, () -> v.validate(new SizeQuantity(2)));
    }
  }

  // ---------- Look wrong, but work ----------

  @Test
  void numberRulesOnStringWorkInHibernateValidator() {
    assertEquals(Set.of(), check(new NumberRulesOnString("40.00")));
    assertEquals(Set.of("amount: must be greater than 0",
            "amount: must be greater than or equal to 5"),
        check(new NumberRulesOnString("-5")));
    assertEquals(Set.of("amount: must be greater than 0",
            "amount: must be greater than or equal to 5",
            "amount: must be less than or equal to 500"),
        check(new NumberRulesOnString("forty")));
  }

  @Test
  void numberRulesOnBigDecimal() {
    assertEquals(Set.of(), check(new NumberRulesOnBigDecimal(new BigDecimal("40.00"))));
    assertEquals(Set.of("amount: must be less than or equal to 500"),
        check(new NumberRulesOnBigDecimal(new BigDecimal("600.00"))));
  }

  @Test
  void numberRulesOnDouble() {
    assertEquals(Set.of("amount: must be greater than 0"), check(new NumberRulesOnDouble(-1.5)));
  }

  @Test
  void sizeOnListCountsElements() {
    assertEquals(Set.of("ccEmails: size must be between 0 and 3"),
        check(new SizeOnList(List.of("a@example.com", "b@example.com", "c@example.com",
            "d@example.com"))));
  }

  // ---------- Fixes ----------

  @Test
  void notNullAndPositiveOnInteger() {
    assertEquals(Set.of("quantity: must not be null"), check(new FixedQuantity(null)));
    assertEquals(Set.of("quantity: must be greater than 0"), check(new FixedQuantity(0)));
    assertEquals(Set.of(), check(new FixedQuantity(2)));
  }

  @Test
  void sizeOnString() {
    assertEquals(Set.of("message: size must be between 0 and 200"),
        check(new FixedMessage("x".repeat(201))));
    assertEquals(Set.of(), check(new FixedMessage("Happy birthday")));
  }

  @Test
  void pastOnLocalDate() {
    assertEquals(Set.of("deliveryDate: must be a past date"),
        check(new FixedPastDate(LocalDate.now().plusDays(1))));
  }

  @Test
  void constraintInsideOptional() {
    assertEquals(Set.of("message: must not be blank"),
        check(new FixedOptionalMessage(Optional.of("  "))));
    assertEquals(Set.of(), check(new FixedOptionalMessage(Optional.of("Thanks"))));
    // An empty Optional gives null to @NotBlank, and null is blank
    assertEquals(Set.of("message: must not be blank"),
        check(new FixedOptionalMessage(Optional.empty())));
  }

  @Test
  void customConstraintValidatorForOwnType() {
    assertEquals(Set.of("recipient: must have a name and an email address"),
        check(new CustomRecipient(new Recipient("Lokesh", "no-at-sign"))));
    assertEquals(Set.of(), check(new CustomRecipient(new Recipient("Lokesh", "lokesh@example.com"))));
  }

  @Test
  void correctedOrderQuantityNull() {
    GiftCardOrder order = new GiftCardOrder(new BigDecimal("40.00"), "anna@example.com",
        "Happy birthday", null, LocalDate.now().plusDays(3));
    assertEquals(Set.of("quantity: must not be null"), check(order));
  }

  @Test
  void correctedOrderMessageTooLong() {
    GiftCardOrder order = new GiftCardOrder(new BigDecimal("40.00"), "anna@example.com",
        "x".repeat(201), 2, LocalDate.now().plusDays(3));
    assertEquals(Set.of("message: size must be between 0 and 200"), check(order));
  }

  @Test
  void correctedOrderIsValid() {
    GiftCardOrder order = new GiftCardOrder(new BigDecimal("40.00"), "anna@example.com",
        "Happy birthday", 2, LocalDate.now().plusDays(3));
    order.setCcEmails(List.of("lokesh@example.com"));
    order.setRecipient(new Recipient("Anna", "anna@example.com"));
    assertEquals(Set.of(), check(order));
  }

  @Test
  void correctedOrderReportsViolationsInsteadOfThrowing() {
    GiftCardOrder bad = new GiftCardOrder(new BigDecimal("0"), "anna", "Hi", 0,
        LocalDate.now().minusDays(1));
    bad.setCcEmails(List.of("ok@example.com", "broken"));
    bad.setRecipient(new Recipient("", "anna@example.com"));
    assertEquals(Set.of(
        "amount: must be greater than 0",
        "ccEmails[1].<list element>: must be a well-formed email address",
        "deliveryDate: must be a date in the present or in the future",
        "quantity: must be greater than or equal to 1",
        "recipient.name: must not be blank",
        "recipientEmail: must be a well-formed email address"), check(bad));
  }

  // ---------- Compile-time check ----------

  @Test
  void annotationProcessorRejectsWrongTypesAtCompileTime(@TempDir Path out) throws Exception {
    String source = """
        import jakarta.validation.constraints.*;
        import java.util.*;
        public class Order {
          @NotBlank Integer quantity;
          @Email List<String> ccEmails;
          @NotBlank Optional<String> message;
          @NotBlank String recipientEmail;
        }
        """;
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///Order.java"),
        JavaFileObject.Kind.SOURCE) {
      @Override
      public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return source;
      }
    };
    List<String> options = List.of("-proc:only",
        "-processor", "org.hibernate.validator.ap.ConstraintValidationProcessor",
        "-cp", System.getProperty("java.class.path"), "-d", out.toString());
    boolean ok = compiler.getTask(null, null, diagnostics, options, null, List.of(file)).call();
    assertFalse(ok);

    List<String> errors = new ArrayList<>();
    for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
      if (d.getKind() == Diagnostic.Kind.ERROR) {
        errors.add(d.getLineNumber() + ": " + d.getMessage(Locale.ENGLISH));
      }
    }
    errors.sort(null);
    assertEquals(List.of(
        "4: The annotation @NotBlank is disallowed for this data type.",
        "5: The annotation @Email is disallowed for this data type.",
        "6: The annotation @NotBlank is disallowed for this data type."), errors);
    assertTrue(Files.isDirectory(out));
  }
}
