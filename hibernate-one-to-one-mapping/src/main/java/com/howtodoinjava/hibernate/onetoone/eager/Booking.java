package com.howtodoinjava.hibernate.onetoone.eager;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking")
public class Booking {

  @Id
  @GeneratedValue
  private Long id;

  private String passenger;

  // Inverse side with the default fetch type (EAGER)
  @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
  private BoardingPass boardingPass;

  protected Booking() {
  }

  public Booking(String passenger) {
    this.passenger = passenger;
  }

  // Keeps both sides in sync
  public void setBoardingPass(BoardingPass pass) {
    if (pass == null) {
      if (this.boardingPass != null) {
        this.boardingPass.setBooking(null);
      }
    } else {
      pass.setBooking(this);
    }
    this.boardingPass = pass;
  }

  public Long getId() {
    return id;
  }

  public String getPassenger() {
    return passenger;
  }

  public BoardingPass getBoardingPass() {
    return boardingPass;
  }
}
