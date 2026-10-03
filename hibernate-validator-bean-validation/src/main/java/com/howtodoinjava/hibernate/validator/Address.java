package com.howtodoinjava.hibernate.validator;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Embeddable
public class Address {

  @NotBlank
  private String city;

  @Pattern(regexp = "[0-9]{5}", message = "must have 5 digits")
  private String zip;

  protected Address() {
  }

  public Address(String city, String zip) {
    this.city = city;
    this.zip = zip;
  }

  public String getCity() {
    return city;
  }

  public String getZip() {
    return zip;
  }
}
