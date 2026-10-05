package com.howtodoinjava.hibernate.batch;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

/** Same columns as SensorReading, but the id comes from an auto-increment (IDENTITY) column. */
@Entity
public class LegacyReading {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String sensorName;
  private LocalDateTime measuredAt;
  private double temperature;

  protected LegacyReading() {
  }

  public LegacyReading(String sensorName, LocalDateTime measuredAt, double temperature) {
    this.sensorName = sensorName;
    this.measuredAt = measuredAt;
    this.temperature = temperature;
  }

  public Long getId() {
    return id;
  }
}
