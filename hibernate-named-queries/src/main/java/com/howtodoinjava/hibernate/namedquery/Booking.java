package com.howtodoinjava.hibernate.namedquery;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQuery(name = "Booking.findByGuest",
    query = "select b from Booking b join fetch b.room where b.guest = :guest")
@org.hibernate.annotations.NamedQuery(name = "Booking.findLongStays",
    query = "select b from Booking b where b.nights >= :nights order by b.guest",
    readOnly = true,
    timeout = 2,
    fetchSize = 50)
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String guest;
  private int nights;

  @ManyToOne(fetch = FetchType.LAZY)
  private Room room;

  protected Booking() {
  }

  public Booking(String guest, int nights, Room room) {
    this.guest = guest;
    this.nights = nights;
    this.room = room;
  }

  public Long getId() {
    return id;
  }

  public String getGuest() {
    return guest;
  }

  public int getNights() {
    return nights;
  }

  public void setNights(int nights) {
    this.nights = nights;
  }

  public Room getRoom() {
    return room;
  }

  @Override
  public String toString() {
    return guest + " in " + room.getNumber() + " for " + nights + " nights";
  }
}
