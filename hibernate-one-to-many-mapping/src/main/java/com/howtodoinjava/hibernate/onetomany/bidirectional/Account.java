package com.howtodoinjava.hibernate.onetomany.bidirectional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String owner;

  // Inverse side: Payment.account owns the account_id foreign key
  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("amount desc")
  private List<Payment> payments = new ArrayList<>();

  protected Account() {
  }

  public Account(String owner) {
    this.owner = owner;
  }

  public void addPayment(Payment payment) {
    payments.add(payment);
    payment.setAccount(this);
  }

  public void removePayment(Payment payment) {
    payments.remove(payment);
    payment.setAccount(null);
  }

  public Payment findPayment(String description) {
    return payments.stream()
        .filter(p -> p.getDescription().equals(description))
        .findFirst()
        .orElseThrow();
  }

  public Long getId() {
    return id;
  }

  public String getOwner() {
    return owner;
  }

  public List<Payment> getPayments() {
    return payments;
  }
}
