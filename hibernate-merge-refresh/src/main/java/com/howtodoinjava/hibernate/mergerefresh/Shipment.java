package com.howtodoinjava.hibernate.mergerefresh;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class Shipment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String destination;

  private String status;

  @Version
  private int version;

  protected Shipment() {
  }

  public Shipment(String destination, String status) {
    this.destination = destination;
    this.status = status;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return "Shipment[id=" + id + ", destination=" + destination + ", status=" + status
        + ", version=" + version + "]";
  }
}
