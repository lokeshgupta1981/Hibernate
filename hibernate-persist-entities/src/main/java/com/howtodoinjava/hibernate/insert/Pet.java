package com.howtodoinjava.hibernate.insert;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Pet {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  private String name;
  private String species;
  private int age;
  private boolean adopted;

  protected Pet() {
  }

  public Pet(String name, String species, int age) {
    this.name = name;
    this.species = species;
    this.age = age;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSpecies() {
    return species;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public boolean isAdopted() {
    return adopted;
  }

  public void setAdopted(boolean adopted) {
    this.adopted = adopted;
  }

  @Override
  public String toString() {
    return name + " (" + species + ", " + age + ")";
  }
}
