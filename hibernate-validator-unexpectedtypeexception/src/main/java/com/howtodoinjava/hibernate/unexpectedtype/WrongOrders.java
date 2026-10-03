package com.howtodoinjava.hibernate.unexpectedtype;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * One small class per mistake (and a few that look wrong but work).
 * A class with one wrong field fails the whole validate() call, so each case stands alone.
 */
public final class WrongOrders {

  private WrongOrders() {
  }

  // ---- These throw UnexpectedTypeException ----

  /** @NotBlank accepts only CharSequence. */
  public static class NotBlankQuantity {
    @NotBlank
    Integer quantity;

    public NotBlankQuantity(Integer quantity) {
      this.quantity = quantity;
    }
  }

  /** @NotEmpty accepts CharSequence, Collection, Map and arrays, not dates. */
  public static class NotEmptyDeliveryDate {
    @NotEmpty
    LocalDate deliveryDate;

    public NotEmptyDeliveryDate(LocalDate deliveryDate) {
      this.deliveryDate = deliveryDate;
    }
  }

  /** @Size measures length or element count, not a numeric value. */
  public static class SizeQuantity {
    @Size(min = 1, max = 10)
    Integer quantity;

    public SizeQuantity(Integer quantity) {
      this.quantity = quantity;
    }
  }

  /** @Email accepts only CharSequence, not our own Recipient type. */
  public static class EmailOnRecipient {
    @Email
    Recipient recipient;

    public EmailOnRecipient(Recipient recipient) {
      this.recipient = recipient;
    }
  }

  /** @Past accepts date and time types, not a String that holds a date. */
  public static class PastOnString {
    @Past
    String deliveryDate;

    public PastOnString(String deliveryDate) {
      this.deliveryDate = deliveryDate;
    }
  }

  /** @Email on the List itself, instead of on its elements. */
  public static class EmailOnList {
    @Email
    List<String> ccEmails;

    public EmailOnList(List<String> ccEmails) {
      this.ccEmails = ccEmails;
    }
  }

  /** @NotBlank on the Optional itself, not on the String inside. */
  public static class NotBlankOnOptional {
    @NotBlank
    Optional<String> message;

    public NotBlankOnOptional(Optional<String> message) {
      this.message = message;
    }
  }

  // ---- These look suspicious but work in Hibernate Validator ----

  /** Hibernate Validator evaluates the number inside the String. Not portable. */
  public static class NumberRulesOnString {
    @Positive
    @Min(5)
    @Max(500)
    String amount;

    public NumberRulesOnString(String amount) {
      this.amount = amount;
    }
  }

  /** The supported way: number rules on BigDecimal. */
  public static class NumberRulesOnBigDecimal {
    @Positive
    @Min(5)
    @Max(500)
    BigDecimal amount;

    public NumberRulesOnBigDecimal(BigDecimal amount) {
      this.amount = amount;
    }
  }

  /** Hibernate Validator accepts any Number subtype, including Double. */
  public static class NumberRulesOnDouble {
    @Positive
    @Max(500)
    Double amount;

    public NumberRulesOnDouble(Double amount) {
      this.amount = amount;
    }
  }

  /** @Size on a List counts the elements. */
  public static class SizeOnList {
    @Size(max = 3)
    List<String> ccEmails;

    public SizeOnList(List<String> ccEmails) {
      this.ccEmails = ccEmails;
    }
  }

  // ---- The fixes ----

  /** @NotNull plus @Positive replaces @NotBlank on a number. */
  public static class FixedQuantity {
    @NotNull
    @Positive
    Integer quantity;

    public FixedQuantity(Integer quantity) {
      this.quantity = quantity;
    }
  }

  /** @Size moved to the String field. */
  public static class FixedMessage {
    @Size(max = 200)
    String message;

    public FixedMessage(String message) {
      this.message = message;
    }
  }

  /** Our own type with its own constraint and ConstraintValidator. */
  public static class CustomRecipient {
    @ValidRecipient
    Recipient recipient;

    public CustomRecipient(Recipient recipient) {
      this.recipient = recipient;
    }
  }

  /** The constraint moved inside Optional, onto the String. */
  public static class FixedOptionalMessage {
    Optional<@NotBlank String> message;

    public FixedOptionalMessage(Optional<String> message) {
      this.message = message;
    }
  }

  /** @Past on a LocalDate. */
  public static class FixedPastDate {
    @Past
    LocalDate deliveryDate;

    public FixedPastDate(LocalDate deliveryDate) {
      this.deliveryDate = deliveryDate;
    }
  }
}
