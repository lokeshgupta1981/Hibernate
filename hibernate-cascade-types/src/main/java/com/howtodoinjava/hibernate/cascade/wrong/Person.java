package com.howtodoinjava.hibernate.cascade.wrong;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** A person who appears in photos. One person can appear in many photos. */
@Entity
public class Person {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  protected Person() {
  }

  public Person(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
