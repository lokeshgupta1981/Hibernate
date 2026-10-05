package com.howtodoinjava.hibernate.jndi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class City {

  @Id
  @GeneratedValue
  private Long id;

  private String name;
  private String country;
  private long population;

  protected City() {
  }

  public City(String name, String country, long population) {
    this.name = name;
    this.country = country;
    this.population = population;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCountry() {
    return country;
  }

  public long getPopulation() {
    return population;
  }

  public void setPopulation(long population) {
    this.population = population;
  }

  @Override
  public String toString() {
    return name + " (" + country + ", " + population + ")";
  }
}
