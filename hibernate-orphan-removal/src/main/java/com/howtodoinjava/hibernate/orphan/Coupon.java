package com.howtodoinjava.hibernate.orphan;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Coupon {

  @Id
  @GeneratedValue
  private Long id;

  private int discount;

  protected Coupon() {
  }

  public Coupon(int discount) {
    this.discount = discount;
  }

  public Long getId() {
    return id;
  }

  public int getDiscount() {
    return discount;
  }
}
