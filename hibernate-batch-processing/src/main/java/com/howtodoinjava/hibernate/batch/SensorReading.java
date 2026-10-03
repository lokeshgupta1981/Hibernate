package com.howtodoinjava.hibernate.batch;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDateTime;

@Entity
public class SensorReading {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(sequenceName = "reading_seq", allocationSize = 50)
  private Long id;

  private String sensorName;
  private LocalDateTime measuredAt;
  private double temperature;

  protected SensorReading() {
  }

  public SensorReading(String sensorName, LocalDateTime measuredAt, double temperature) {
    this.sensorName = sensorName;
    this.measuredAt = measuredAt;
    this.temperature = temperature;
  }

  public Long getId() {
    return id;
  }

  public String getSensorName() {
    return sensorName;
  }

  public LocalDateTime getMeasuredAt() {
    return measuredAt;
  }

  public double getTemperature() {
    return temperature;
  }

  public void setTemperature(double temperature) {
    this.temperature = temperature;
  }

  @Override
  public String toString() {
    return sensorName + " " + measuredAt + " " + temperature;
  }
}
