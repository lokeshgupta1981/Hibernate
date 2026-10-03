package com.howtodoinjava.hibernate.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity
public class MenuItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private BigDecimal price;

  @Basic(fetch = FetchType.LAZY)   // ignored: this class is not bytecode enhanced
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "restaurant_id")
  private Restaurant restaurant;

  protected MenuItem() {
  }

  public MenuItem(String name, String price) {
    this.name = name;
    this.price = new BigDecimal(price);
    this.description = name + " of the day";
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public String getDescription() {
    return description;
  }

  public Restaurant getRestaurant() {
    return restaurant;
  }

  void setRestaurant(Restaurant restaurant) {
    this.restaurant = restaurant;
  }

  @Override
  public String toString() {
    return name;
  }
}
