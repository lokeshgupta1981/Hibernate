package com.howtodoinjava.hibernate.onetoone.eager;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "boarding_pass")
public class BoardingPass {

  @Id
  @GeneratedValue
  private Long id;

  private String seat;

  // Owning side with the default fetch type (EAGER)
  @OneToOne
  @JoinColumn(name = "booking_id")
  private Booking booking;

  protected BoardingPass() {
  }

  public BoardingPass(String seat) {
    this.seat = seat;
  }

  public Long getId() {
    return id;
  }

  public String getSeat() {
    return seat;
  }

  public Booking getBooking() {
    return booking;
  }

  public void setBooking(Booking booking) {
    this.booking = booking;
  }
}
