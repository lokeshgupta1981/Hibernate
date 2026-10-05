package com.howtodoinjava.hibernate.testing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Invoice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String client;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(name = "issued_on", nullable = false)
  private LocalDate issuedOn;

  private boolean paid;

  protected Invoice() {
  }

  public Invoice(String client, String amount, LocalDate issuedOn) {
    this.client = client;
    this.amount = new BigDecimal(amount);
    this.issuedOn = issuedOn;
  }

  public Long getId() {
    return id;
  }

  public String getClient() {
    return client;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDate getIssuedOn() {
    return issuedOn;
  }

  public boolean isPaid() {
    return paid;
  }

  public void markPaid() {
    this.paid = true;
  }

  @Override
  public String toString() {
    return client + " " + amount + (paid ? " (paid)" : " (open)");
  }
}
