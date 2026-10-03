package com.howtodoinjava.hibernate.genericjdbc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The correct mapping: table and column names that are not SQL keywords.
 */
@Entity
@Table(name = "cake_order")
public class CakeOrder {

  @Id
  @GeneratedValue
  private Long id;

  private String customerName;
  private String flavor;
  private LocalDate pickupDate;

  @Column(name = "order_value")
  private BigDecimal value;

  protected CakeOrder() {
  }

  public CakeOrder(String customerName, String flavor, LocalDate pickupDate, BigDecimal value) {
    this.customerName = customerName;
    this.flavor = flavor;
    this.pickupDate = pickupDate;
    this.value = value;
  }

  public Long getId() {
    return id;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getFlavor() {
    return flavor;
  }

  public LocalDate getPickupDate() {
    return pickupDate;
  }

  public BigDecimal getValue() {
    return value;
  }
}
