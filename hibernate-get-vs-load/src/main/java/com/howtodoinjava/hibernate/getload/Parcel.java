package com.howtodoinjava.hibernate.getload;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Parcel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String trackingCode;
  private String status;
  private double weightKg;

  public Parcel() {
  }

  public Parcel(String trackingCode, String status, double weightKg) {
    this.trackingCode = trackingCode;
    this.status = status;
    this.weightKg = weightKg;
  }

  public Long getId() {
    return id;
  }

  public String getTrackingCode() {
    return trackingCode;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public double getWeightKg() {
    return weightKg;
  }

  @Override
  public String toString() {
    return "Parcel[" + trackingCode + ", " + status + ", " + weightKg + " kg]";
  }
}
