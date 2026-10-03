package com.howtodoinjava.hibernate.emf;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Drink {

  @Id
  @GeneratedValue
  private Long id;

  private String name;
  private String size;
  private BigDecimal price;

  protected Drink() {
  }

  public Drink(String name, String size, String price) {
    this.name = name;
    this.size = size;
    this.price = new BigDecimal(price);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSize() {
    return size;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  @Override
  public String toString() {
    return name + " (" + size + ", " + price + ")";
  }
}
