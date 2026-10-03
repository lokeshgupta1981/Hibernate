package com.howtodoinjava.hibernate.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

  @Column(length = 100)
  private String street;

  @Column(length = 50)
  private String city;

  @Column(length = 10)
  private String zip;

  protected Address() {
  }

  public Address(String street, String city, String zip) {
    this.street = street;
    this.city = city;
    this.zip = zip;
  }

  public String getStreet() {
    return street;
  }

  public String getCity() {
    return city;
  }

  public String getZip() {
    return zip;
  }
}
