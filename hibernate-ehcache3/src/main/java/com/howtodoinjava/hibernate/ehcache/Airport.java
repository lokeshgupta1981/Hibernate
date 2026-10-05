package com.howtodoinjava.hibernate.ehcache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Airport {

  @Id
  private String iataCode;

  private String name;
  private String city;

  protected Airport() {
  }

  public Airport(String iataCode, String name, String city) {
    this.iataCode = iataCode;
    this.name = name;
    this.city = city;
  }

  public String getIataCode() {
    return iataCode;
  }

  public String getName() {
    return name;
  }

  public String getCity() {
    return city;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return iataCode + " (" + city + ")";
  }
}
