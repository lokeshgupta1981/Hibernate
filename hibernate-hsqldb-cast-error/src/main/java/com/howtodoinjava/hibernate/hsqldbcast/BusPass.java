package com.howtodoinjava.hibernate.hsqldbcast;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "bus_pass")
public class BusPass {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String passNumber;
  private String holderName;
  private int zone;
  private LocalDate validUntil;

  @Enumerated(EnumType.STRING)
  private PassType type;

  protected BusPass() {
  }

  public BusPass(String passNumber, String holderName, int zone, LocalDate validUntil, PassType type) {
    this.passNumber = passNumber;
    this.holderName = holderName;
    this.zone = zone;
    this.validUntil = validUntil;
    this.type = type;
  }

  public Long getId() {
    return id;
  }

  public String getPassNumber() {
    return passNumber;
  }

  public String getHolderName() {
    return holderName;
  }

  public int getZone() {
    return zone;
  }

  public LocalDate getValidUntil() {
    return validUntil;
  }

  public PassType getType() {
    return type;
  }

  @Override
  public String toString() {
    return passNumber + " (" + holderName + ", zone " + zone + ", " + type + ", " + validUntil + ")";
  }
}
