package com.howtodoinjava.hibernate.onetoone.sharedkey;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// No reference to BoardingPass: we load the pass with the booking id
@Entity
@Table(name = "booking")
public class Booking {

  @Id
  @GeneratedValue
  private Long id;

  private String passenger;

  protected Booking() {
  }

  public Booking(String passenger) {
    this.passenger = passenger;
  }

  public Long getId() {
    return id;
  }

  public String getPassenger() {
    return passenger;
  }
}
