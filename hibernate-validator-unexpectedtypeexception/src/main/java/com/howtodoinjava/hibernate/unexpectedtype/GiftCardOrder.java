package com.howtodoinjava.hibernate.unexpectedtype;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A gift card order where every constraint matches the type of its field.
 */
public class GiftCardOrder {

  @NotNull
  @Positive
  private BigDecimal amount;

  @NotBlank
  @Email
  private String recipientEmail;

  @Size(max = 200)
  private String message;

  @NotNull
  @Min(1)
  @Max(10)
  private Integer quantity;

  @NotNull
  @FutureOrPresent
  private LocalDate deliveryDate;

  @Size(max = 3)
  private List<@Email String> ccEmails = new ArrayList<>();

  @Valid
  private Recipient recipient;

  public GiftCardOrder() {
  }

  public GiftCardOrder(BigDecimal amount, String recipientEmail, String message, Integer quantity,
      LocalDate deliveryDate) {
    this.amount = amount;
    this.recipientEmail = recipientEmail;
    this.message = message;
    this.quantity = quantity;
    this.deliveryDate = deliveryDate;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getRecipientEmail() {
    return recipientEmail;
  }

  public void setRecipientEmail(String recipientEmail) {
    this.recipientEmail = recipientEmail;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public LocalDate getDeliveryDate() {
    return deliveryDate;
  }

  public void setDeliveryDate(LocalDate deliveryDate) {
    this.deliveryDate = deliveryDate;
  }

  public List<String> getCcEmails() {
    return ccEmails;
  }

  public void setCcEmails(List<String> ccEmails) {
    this.ccEmails = ccEmails;
  }

  public Recipient getRecipient() {
    return recipient;
  }

  public void setRecipient(Recipient recipient) {
    this.recipient = recipient;
  }
}
