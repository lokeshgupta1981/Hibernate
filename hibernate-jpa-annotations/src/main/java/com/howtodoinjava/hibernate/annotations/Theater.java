package com.howtodoinjava.hibernate.annotations;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Theater {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80, unique = true)
  private String name;

  @Column(columnDefinition = "smallint default 100 not null")
  private int seats;

  @Embedded
  @AttributeOverride(name = "zip", column = @Column(name = "postal_code", length = 10))
  private Address address;

  @Embedded
  private BoxOffice boxOffice;

  protected Theater() {
  }

  public Theater(String name, int seats, Address address, BoxOffice boxOffice) {
    this.name = name;
    this.seats = seats;
    this.address = address;
    this.boxOffice = boxOffice;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getSeats() {
    return seats;
  }

  public Address getAddress() {
    return address;
  }

  public BoxOffice getBoxOffice() {
    return boxOffice;
  }
}
