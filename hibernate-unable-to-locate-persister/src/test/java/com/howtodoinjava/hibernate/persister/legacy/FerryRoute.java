package com.howtodoinjava.hibernate.persister.legacy;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

// Cause 3: old Java EE annotations; Hibernate 6 and 7 read only jakarta.persistence
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
}
