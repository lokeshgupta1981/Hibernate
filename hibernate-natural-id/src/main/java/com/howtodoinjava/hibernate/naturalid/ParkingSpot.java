package com.howtodoinjava.hibernate.naturalid;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.NaturalIdClass;

@Entity
@NaturalIdClass(SpotKey.class)
@NaturalIdCache
public class ParkingSpot {

  @Id
  @GeneratedValue
  private Long id;

  @NaturalId
  private String garage;

  @NaturalId
  private int number;

  private boolean covered;

  protected ParkingSpot() {
  }

  public ParkingSpot(String garage, int number, boolean covered) {
    this.garage = garage;
    this.number = number;
    this.covered = covered;
  }

  public Long getId() {
    return id;
  }

  public String getGarage() {
    return garage;
  }

  public int getNumber() {
    return number;
  }

  public boolean isCovered() {
    return covered;
  }

  @Override
  public String toString() {
    return "ParkingSpot(" + garage + ", " + number + ")";
  }
}
