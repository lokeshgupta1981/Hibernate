package com.howtodoinjava.hibernate.c3p0;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class ParkingEntry {

  @Id
  @GeneratedValue
  private Long id;

  private String plate;

  private LocalDateTime enteredAt;

  protected ParkingEntry() {
  }

  public ParkingEntry(String plate, LocalDateTime enteredAt) {
    this.plate = plate;
    this.enteredAt = enteredAt;
  }

  public Long getId() {
    return id;
  }

  public String getPlate() {
    return plate;
  }

  public LocalDateTime getEnteredAt() {
    return enteredAt;
  }
}
