package com.howtodoinjava.hibernate.notfound;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Scholarship {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private BigDecimal amount;

  protected Scholarship() {
  }

  public Scholarship(String name, BigDecimal amount) {
    this.name = name;
    this.amount = amount;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  @Override
  public String toString() {
    return "Scholarship[" + name + ", " + amount + "]";
  }
}
