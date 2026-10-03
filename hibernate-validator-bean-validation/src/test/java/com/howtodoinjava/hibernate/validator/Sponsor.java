package com.howtodoinjava.hibernate.validator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

/** Entity with an IDENTITY id: Hibernate runs its INSERT inside persist(). */
@Entity
public class Sponsor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  private String name;

  protected Sponsor() {
  }

  public Sponsor(String name) {
    this.name = name;
  }
}
