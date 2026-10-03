package com.howtodoinjava.hibernate.interceptors;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Appointment {

  @Id
  @GeneratedValue
  private Long id;

  private String patientName;
  private String doctor;
  private LocalDateTime slot;
  private String status;

  protected Appointment() {
  }

  public Appointment(String patientName, String doctor, LocalDateTime slot) {
    this.patientName = patientName;
    this.doctor = doctor;
    this.slot = slot;
  }

  public Long getId() {
    return id;
  }

  public String getPatientName() {
    return patientName;
  }

  public String getDoctor() {
    return doctor;
  }

  public void setDoctor(String doctor) {
    this.doctor = doctor;
  }

  public LocalDateTime getSlot() {
    return slot;
  }

  public void setSlot(LocalDateTime slot) {
    this.slot = slot;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return "Appointment[" + patientName + ", " + doctor + ", " + slot + ", " + status + "]";
  }
}
