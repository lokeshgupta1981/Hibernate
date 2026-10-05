package com.howtodoinjava.hibernate.genericjdbc;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The broken mapping: the table name ORDER and the column name VALUE are SQL keywords.
 */
@Entity
public class Order {

  @Id
  @GeneratedValue
  private Long id;

  private String customerName;
  private String flavor;
  private LocalDate pickupDate;
  private BigDecimal value;

  protected Order() {
  }

  public Order(String customerName, String flavor, LocalDate pickupDate, BigDecimal value) {
    this.customerName = customerName;
    this.flavor = flavor;
    this.pickupDate = pickupDate;
    this.value = value;
  }

  public Long getId() {
    return id;
  }
}
