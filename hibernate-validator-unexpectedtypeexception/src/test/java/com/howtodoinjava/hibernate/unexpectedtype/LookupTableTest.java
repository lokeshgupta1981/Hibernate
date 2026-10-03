package com.howtodoinjava.hibernate.unexpectedtype;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.validation.UnexpectedTypeException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Checks the extra cells of the lookup table in the article (types and constraints not in the grid image).
 */
class LookupTableTest {

  static class Works {
    @Pattern(regexp = "[a-z]+") String text;
    @Digits(integer = 3, fraction = 2) BigDecimal amount;
    @Digits(integer = 2, fraction = 0) Integer quantity;
    @Min(1) @Max(10) @Positive @Digits(integer = 2, fraction = 0) Long count;
    @Future LocalDate date;
    @Past LocalDate pastDate;
    @Future LocalDateTime dateTime;
    @FutureOrPresent Instant instant;
    @NotEmpty @Size(max = 3) Set<String> tags;
    @NotEmpty @Size(max = 3) Map<String, String> notes;
    @NotNull Recipient recipient;
  }

  static class FutureOnString {
    @Future String date;
  }

  static class NotBlankOnSet {
    @NotBlank Set<String> tags;
  }

  static class NotBlankOnLong {
    @NotBlank Long count;
  }

  static class SizeOnLong {
    @Size(max = 3) Long count;
  }

  static class EmailOnMap {
    @Email Map<String, String> notes;
  }

  static class SizeOnLocalDateTime {
    @Size(max = 3) LocalDateTime dateTime;
  }

  static class PositiveOnRecipient {
    @Positive Recipient recipient;
  }

  @Test
  void lookupTable() {
    try (ValidatorFactory factory = Validators.create()) {
      Validator v = factory.getValidator();
      // All fields null: only the null and empty checks report, nothing throws
      assertEquals(java.util.Set.of("notes: must not be empty", "recipient: must not be null",
          "tags: must not be empty"), assertDoesNotThrow(() -> Validators.check(v, new Works())));
      assertThrows(UnexpectedTypeException.class, () -> v.validate(new FutureOnString()));
      assertThrows(UnexpectedTypeException.class, () -> v.validate(new NotBlankOnSet()));
      assertThrows(UnexpectedTypeException.class, () -> v.validate(new SizeOnLocalDateTime()));
      assertThrows(UnexpectedTypeException.class, () -> v.validate(new NotBlankOnLong()));
      assertThrows(UnexpectedTypeException.class, () -> v.validate(new SizeOnLong()));
      assertThrows(UnexpectedTypeException.class, () -> v.validate(new EmailOnMap()));
      assertThrows(UnexpectedTypeException.class, () -> v.validate(new PositiveOnRecipient()));
    }
  }
}
