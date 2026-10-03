package com.howtodoinjava.hibernate.persister;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class FerryRoute {

  @Id
  @GeneratedValue
  private Long id;

  private String fromPort;
  private String toPort;
  private int durationMinutes;

  protected FerryRoute() {
  }

  public FerryRoute(String fromPort, String toPort, int durationMinutes) {
    this.fromPort = fromPort;
    this.toPort = toPort;
    this.durationMinutes = durationMinutes;
  }

  public Long getId() {
    return id;
  }

  public String getFromPort() {
    return fromPort;
  }

  public String getToPort() {
    return toPort;
  }

  public int getDurationMinutes() {
    return durationMinutes;
  }

  @Override
  public String toString() {
    return fromPort + " -> " + toPort + " (" + durationMinutes + " min)";
  }
}
