package com.howtodoinjava.hibernate.genericjdbc;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Cake order with an IDENTITY id: Hibernate asks the driver for generated keys,
 * which HSQLDB 1.8 does not support.
 */
@Entity
@Table(name = "identity_order")
public class IdentityOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String flavor;

  protected IdentityOrder() {
  }

  public IdentityOrder(String flavor) {
    this.flavor = flavor;
  }

  public Long getId() {
    return id;
  }
}
