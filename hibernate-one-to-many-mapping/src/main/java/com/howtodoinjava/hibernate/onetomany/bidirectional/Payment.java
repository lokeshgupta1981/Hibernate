package com.howtodoinjava.hibernate.onetomany.bidirectional;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String description;

  private BigDecimal amount;

  // Owning side: this field decides the value of account_id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id")
  private Account account;

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

  public Account getAccount() {
    return account;
  }

  void setAccount(Account account) {
    this.account = account;
  }

  @Override
  public String toString() {
    return description + " " + amount;
  }
}
