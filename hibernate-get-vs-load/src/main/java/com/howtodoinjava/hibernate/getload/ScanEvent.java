package com.howtodoinjava.hibernate.getload;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** A scan of a parcel at a depot. Only the parcel's id is stored (parcel_id). */
@Entity
public class ScanEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String location;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parcel_id")
  private Parcel parcel;

  protected ScanEvent() {
  }

  public ScanEvent(String location, Parcel parcel) {
    this.location = location;
    this.parcel = parcel;
  }

  public Long getId() {
    return id;
  }

  public String getLocation() {
    return location;
  }

  public Parcel getParcel() {
    return parcel;
  }
}
