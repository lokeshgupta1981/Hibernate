package com.howtodoinjava.hibernate.onetomany.joincolumn;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String description;

  private BigDecimal amount;

  protected Payment() {
  }

  public Payment(String description, String amount) {
    this.description = description;
    this.amount = new BigDecimal(amount);
  }

  public Long getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  @Override
  public String toString() {
    return description + " " + amount;
  }
}
