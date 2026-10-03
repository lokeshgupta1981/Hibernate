package com.howtodoinjava.hibernate.parameters;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalTime;

@Entity
public class TrainService {

  @Id
  @GeneratedValue
  private Long id;

  private int trainNumber;
  private String fromStation;
  private String toStation;
  private LocalTime departsAt;

  protected TrainService() {
  }

  public TrainService(int trainNumber, String fromStation, String toStation, LocalTime departsAt) {
    this.trainNumber = trainNumber;
    this.fromStation = fromStation;
    this.toStation = toStation;
    this.departsAt = departsAt;
  }

  public Long getId() {
    return id;
  }

  public int getTrainNumber() {
    return trainNumber;
  }

  public String getFromStation() {
    return fromStation;
  }

  public String getToStation() {
    return toStation;
  }

  public LocalTime getDepartsAt() {
    return departsAt;
  }

  @Override
  public String toString() {
    return trainNumber + " " + fromStation + "-" + toStation + " " + departsAt;
  }
}
