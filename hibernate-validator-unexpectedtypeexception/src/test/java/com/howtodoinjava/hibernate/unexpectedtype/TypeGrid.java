package com.howtodoinjava.hibernate.unexpectedtype;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Every combination of field type and constraint shown in the compatibility grid.
 * The test validates each property alone and records whether Hibernate Validator throws.
 */
final class TypeGrid {

  static final List<String> COLUMNS = List.of("notNull", "notBlank", "notEmpty", "size", "email", "min", "positive", "past");

  private TypeGrid() {
  }


  static class StringField {
    @NotNull String notNull;
    @NotBlank String notBlank;
    @NotEmpty String notEmpty;
    @Size(max = 3) String size;
    @Email String email;
    @Min(1) String min;
    @Positive String positive;
    @Past String past;
  }

  static class IntegerField {
    @NotNull Integer notNull;
    @NotBlank Integer notBlank;
    @NotEmpty Integer notEmpty;
    @Size(max = 3) Integer size;
    @Email Integer email;
    @Min(1) Integer min;
    @Positive Integer positive;
    @Past Integer past;
  }

  static class BigDecimalField {
    @NotNull BigDecimal notNull;
    @NotBlank BigDecimal notBlank;
    @NotEmpty BigDecimal notEmpty;
    @Size(max = 3) BigDecimal size;
    @Email BigDecimal email;
    @Min(1) BigDecimal min;
    @Positive BigDecimal positive;
    @Past BigDecimal past;
  }

  static class DoubleField {
    @NotNull Double notNull;
    @NotBlank Double notBlank;
    @NotEmpty Double notEmpty;
    @Size(max = 3) Double size;
    @Email Double email;
    @Min(1) Double min;
    @Positive Double positive;
    @Past Double past;
  }

  static class LocalDateField {
    @NotNull LocalDate notNull;
    @NotBlank LocalDate notBlank;
    @NotEmpty LocalDate notEmpty;
    @Size(max = 3) LocalDate size;
    @Email LocalDate email;
    @Min(1) LocalDate min;
    @Positive LocalDate positive;
    @Past LocalDate past;
  }

  static class ListField {
    @NotNull List<String> notNull;
    @NotBlank List<String> notBlank;
    @NotEmpty List<String> notEmpty;
    @Size(max = 3) List<String> size;
    @Email List<String> email;
    @Min(1) List<String> min;
    @Positive List<String> positive;
    @Past List<String> past;
  }

  static class OptionalField {
    @NotNull Optional<String> notNull;
    @NotBlank Optional<String> notBlank;
    @NotEmpty Optional<String> notEmpty;
    @Size(max = 3) Optional<String> size;
    @Email Optional<String> email;
    @Min(1) Optional<String> min;
    @Positive Optional<String> positive;
    @Past Optional<String> past;
  }

  static class RecipientField {
    @NotNull Recipient notNull;
    @NotBlank Recipient notBlank;
    @NotEmpty Recipient notEmpty;
    @Size(max = 3) Recipient size;
    @Email Recipient email;
    @Min(1) Recipient min;
    @Positive Recipient positive;
    @Past Recipient past;
  }
}
