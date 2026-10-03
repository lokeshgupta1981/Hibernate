package com.howtodoinjava.hibernate.onetomany.joincolumn;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String owner;

  // Unidirectional: the parent owns the account_id column in the Payment table
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "account_id")
  private List<Payment> payments = new ArrayList<>();

  protected Account() {
  }

  public Account(String owner) {
    this.owner = owner;
  }

  public Long getId() {
    return id;
  }

  public List<Payment> getPayments() {
    return payments;
  }

  public Payment findPayment(String description) {
    return payments.stream()
        .filter(p -> p.getDescription().equals(description))
        .findFirst()
        .orElseThrow();
  }
}
