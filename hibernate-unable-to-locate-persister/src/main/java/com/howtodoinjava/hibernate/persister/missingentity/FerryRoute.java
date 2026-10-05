package com.howtodoinjava.hibernate.persister.missingentity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

// Cause 2: the class has JPA field annotations but no @Entity
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
