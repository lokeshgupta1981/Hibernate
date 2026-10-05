package com.howtodoinjava.hibernate.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Neighborhood {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String city;

  protected Neighborhood() {
  }

  public Neighborhood(String name, String city) {
    this.name = name;
    this.city = city;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCity() {
    return city;
  }

  @Override
  public String toString() {
    return name;
  }
}
