package com.howtodoinjava.hibernate.hsqldbcast;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * The same bus_pass table, mapped with the default enum mapping (ORDINAL): no @Enumerated.
 * Used to show what happens when the column holds names such as 'MONTHLY'.
 */
@Entity(name = "OrdinalBusPass")
@Table(name = "bus_pass")
public class OrdinalBusPass {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String passNumber;
  private String holderName;
  private int zone;
  private LocalDate validUntil;

  private PassType type;     // no @Enumerated: ORDINAL, an integer 0, 1, 2

  protected OrdinalBusPass() {
  }

  public OrdinalBusPass(String passNumber, String holderName, int zone, LocalDate validUntil, PassType type) {
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

  public PassType getType() {
    return type;
  }
}
