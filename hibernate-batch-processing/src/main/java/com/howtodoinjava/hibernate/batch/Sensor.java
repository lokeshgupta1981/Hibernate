package com.howtodoinjava.hibernate.batch;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Sensor {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(sequenceName = "sensor_seq", allocationSize = 50)
  private Long id;

  private String name;
  private String location;

  @Version
  private int version;

  // Read-only view of the readings through the sensorName column; cascade persists new readings.
  @OneToMany(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "sensorName", referencedColumnName = "name", insertable = false, updatable = false,
      foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
  private List<SensorReading> readings = new ArrayList<>();

  protected Sensor() {
  }

  public Sensor(String name, String location) {
    this.name = name;
    this.location = location;
  }

  public void addReading(SensorReading reading) {
    readings.add(reading);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public List<SensorReading> getReadings() {
    return readings;
  }
}
