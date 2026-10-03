package com.howtodoinjava.hibernate.unexpectedtype;

import com.howtodoinjava.hibernate.unexpectedtype.WrongOrders.*;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class UnexpectedTypeDemo {

  public static void main(String[] args) {
    try (ValidatorFactory factory = Validators.create()) {
      Validator validator = factory.getValidator();

      step("1. @NotBlank on Integer quantity");
      run(validator, new NotBlankQuantity(2));

      step("2. Same field, value null: still throws");
      run(validator, new NotBlankQuantity(null));

      step("3. @NotEmpty on LocalDate deliveryDate");
      run(validator, new NotEmptyDeliveryDate(LocalDate.of(2026, 12, 24)));

      step("4. @Size on Integer quantity");
      run(validator, new SizeQuantity(2));

      step("5. @Email on our own Recipient type");
      run(validator, new EmailOnRecipient(new Recipient("Lokesh", "lokesh@example.com")));

      step("6. @Past on a String date");
      run(validator, new PastOnString("2026-01-15"));

      step("7. @Email on List<String> instead of its elements");
      run(validator, new EmailOnList(List.of("anna@example.com")));

      step("8. @Positive @Min(5) @Max(500) on String amount: works in Hibernate Validator");
      run(validator, new NumberRulesOnString("40.00"));
      run(validator, new NumberRulesOnString("-5"));
      run(validator, new NumberRulesOnString("forty"));

      step("9. Same rules on BigDecimal");
      run(validator, new NumberRulesOnBigDecimal(new BigDecimal("40.00")));
      run(validator, new NumberRulesOnBigDecimal(new BigDecimal("600.00")));

      step("10. @Positive @Max(500) on Double");
      run(validator, new NumberRulesOnDouble(-1.5));

      step("11. @Size on List counts elements");
      run(validator, new SizeOnList(List.of("a@example.com", "b@example.com", "c@example.com",
          "d@example.com")));

      step("12. @NotBlank on Optional<String>");
      run(validator, new NotBlankOnOptional(Optional.of("  ")));

      step("13. Fixes");
      run(validator, new FixedOptionalMessage(Optional.of("  ")));
      run(validator, new FixedOptionalMessage(Optional.empty()));
      run(validator, new FixedQuantity(null));
      run(validator, new FixedQuantity(0));
      run(validator, new FixedMessage("x".repeat(201)));
      run(validator, new FixedPastDate(LocalDate.now().plusDays(1)));
      run(validator, new CustomRecipient(new Recipient("Lokesh", "no-at-sign")));

      step("14. The corrected GiftCardOrder");
      GiftCardOrder order = new GiftCardOrder(new BigDecimal("40.00"), "anna@example.com",
          "Happy birthday", 2, LocalDate.now().plusDays(3));
      run(validator, order);
      GiftCardOrder bad = new GiftCardOrder(new BigDecimal("0"), "anna", "Hi", 0,
          LocalDate.now().minusDays(1));
      bad.setCcEmails(List.of("ok@example.com", "broken"));
      bad.setRecipient(new Recipient("", "anna@example.com"));
      run(validator, bad);
    }
  }

  private static void run(Validator validator, Object bean) {
    try {
      System.out.println("  violations: " + Validators.check(validator, bean));
    } catch (RuntimeException e) {
      System.out.println("  " + e.getClass().getName() + ": " + e.getMessage());
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println(title);
  }
}
