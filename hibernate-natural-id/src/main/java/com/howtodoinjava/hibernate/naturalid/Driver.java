package com.howtodoinjava.hibernate.naturalid;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.NaturalId;

@Entity
public class Driver {

  @Id
  @GeneratedValue
  private Long id;

  @NaturalId(mutable = true)
  private String licenseNumber;

  private String name;

  protected Driver() {
  }

  public Driver(String licenseNumber, String name) {
    this.licenseNumber = licenseNumber;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getLicenseNumber() {
    return licenseNumber;
  }

  public void setLicenseNumber(String licenseNumber) {
    this.licenseNumber = licenseNumber;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "Driver(" + licenseNumber + ", " + name + ")";
  }
}
