package com.howtodoinjava.hibernate.namedqueryerrors;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Volunteer {

  @Id
  @GeneratedValue
  private Long id;

  private String name;
  private String phone;

  protected Volunteer() {
  }

  public Volunteer(String name, String phone) {
    this.name = name;
    this.phone = phone;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }
}
