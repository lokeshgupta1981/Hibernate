package com.howtodoinjava.hibernate.onetoone.primarykey;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking")
public class Booking {

  @Id
  @GeneratedValue
  private Long id;

  private String passenger;

  // Joins booking.id = boarding_pass.id, but does not copy the id
  @OneToOne(cascade = CascadeType.ALL)
  @PrimaryKeyJoinColumn
  private BoardingPass boardingPass;

  protected Booking() {
  }

  public Booking(String passenger) {
    this.passenger = passenger;
  }

  public Long getId() {
    return id;
  }

  public BoardingPass getBoardingPass() {
    return boardingPass;
  }

  public void setBoardingPass(BoardingPass boardingPass) {
    this.boardingPass = boardingPass;
  }
}
