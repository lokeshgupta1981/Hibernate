package com.howtodoinjava.hibernate.onetoone.sharedkey;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "boarding_pass")
public class BoardingPass {

  // No @GeneratedValue: @MapsId copies the id of the booking
  @Id
  private Long id;

  private String seat;

  // booking_id is both the primary key and the foreign key
  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "booking_id")
  private Booking booking;

  protected BoardingPass() {
  }

  public BoardingPass(Booking booking, String seat) {
    this.booking = booking;
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
}
