package com.howtodoinjava.hibernate.orphan;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Nutrition {

  @Id
  @GeneratedValue
  private Long id;

  private int calories;

  protected Nutrition() {
  }

  public Nutrition(int calories) {
    this.calories = calories;
  }

  public Long getId() {
    return id;
  }

  public int getCalories() {
    return calories;
  }
}
