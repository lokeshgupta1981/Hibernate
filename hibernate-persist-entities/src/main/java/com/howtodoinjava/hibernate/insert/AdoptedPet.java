package com.howtodoinjava.hibernate.insert;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AdoptedPet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String species;

  protected AdoptedPet() {
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSpecies() {
    return species;
  }
}
