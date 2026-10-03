package com.howtodoinjava.hibernate.saveorupdate.legacy;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Plant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String species;
  private String potSize;
  private double price;

  protected Plant() {
  }

  public Plant(String name, String species, String potSize, double price) {
    this.name = name;
    this.species = species;
    this.potSize = potSize;
    this.price = price;
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

  public String getSpecies() {
    return species;
  }

  public String getPotSize() {
    return potSize;
  }

  public void setPotSize(String potSize) {
    this.potSize = potSize;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  @Override
  public String toString() {
    return "Plant[id=" + id + ", name=" + name + ", potSize=" + potSize + ", price=" + price + "]";
  }
}
