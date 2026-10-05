package com.howtodoinjava.hibernate.insert;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Kennel {

  @Id
  private Integer number;

  private String size;

  protected Kennel() {
  }

  public Kennel(Integer number, String size) {
    this.number = number;
    this.size = size;
  }

  public Integer getNumber() {
    return number;
  }

  public String getSize() {
    return size;
  }
}
