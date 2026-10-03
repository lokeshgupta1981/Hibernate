package com.howtodoinjava.hibernate.nativeselect;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Farm {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String region;

  protected Farm() {
  }

  public Farm(String name, String region) {
    this.name = name;
    this.region = region;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getRegion() {
    return region;
  }

  @Override
  public String toString() {
    return name + " (" + region + ")";
  }
}
