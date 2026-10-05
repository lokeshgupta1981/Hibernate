package com.howtodoinjava.hibernate.naturalid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

@Entity
@NaturalIdCache
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Vehicle {

  @Id
  @GeneratedValue
  private Long id;

  @NaturalId
  @Column(nullable = false)
  private String licensePlate;

  private String model;

  protected Vehicle() {
  }

  public Vehicle(String licensePlate, String model) {
    this.licensePlate = licensePlate;
    this.model = model;
  }

  public Long getId() {
    return id;
  }

  public String getLicensePlate() {
    return licensePlate;
  }

  public void setLicensePlate(String licensePlate) {
    this.licensePlate = licensePlate;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Vehicle other)) {
      return false;
    }
    return licensePlate != null && licensePlate.equals(other.licensePlate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hashCode(licensePlate);
  }

  @Override
  public String toString() {
    return "Vehicle(" + licensePlate + ", " + model + ")";
  }
}
