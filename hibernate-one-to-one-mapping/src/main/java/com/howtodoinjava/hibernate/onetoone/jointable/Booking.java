package com.howtodoinjava.hibernate.onetoone.jointable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking")
public class Booking {

  @Id
  @GeneratedValue
  private Long id;

  private String passenger;

  // The link lives in its own table; neither booking nor boarding_pass has a foreign key
  @OneToOne(cascade = CascadeType.ALL)
  @JoinTable(name = "booking_boarding_pass",
      joinColumns = @JoinColumn(name = "booking_id"),
      inverseJoinColumns = @JoinColumn(name = "boarding_pass_id"))
  private BoardingPass boardingPass;

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

  public BoardingPass getBoardingPass() {
    return boardingPass;
  }

  public void setBoardingPass(BoardingPass boardingPass) {
    this.boardingPass = boardingPass;
  }
}
