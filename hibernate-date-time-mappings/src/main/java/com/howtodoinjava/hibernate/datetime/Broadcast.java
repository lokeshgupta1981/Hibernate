package com.howtodoinjava.hibernate.datetime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.ZonedDateTime;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

/** One webinar start time stored with each TimeZoneStorageType. */
@Entity
public class Broadcast {

  @Id
  @GeneratedValue
  private Long id;

  private ZonedDateTime defaultStart;

  @TimeZoneStorage(TimeZoneStorageType.NATIVE)
  private ZonedDateTime nativeStart;

  @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
  private ZonedDateTime normalizedStart;

  @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
  private ZonedDateTime utcStart;

  @TimeZoneStorage(TimeZoneStorageType.COLUMN)
  @TimeZoneColumn(name = "columnStart_offset")
  private ZonedDateTime columnStart;

  protected Broadcast() {
  }

  public Broadcast(ZonedDateTime start) {
    this.defaultStart = start;
    this.nativeStart = start;
    this.normalizedStart = start;
    this.utcStart = start;
    this.columnStart = start;
  }

  public Long getId() {
    return id;
  }

  public ZonedDateTime getDefaultStart() {
    return defaultStart;
  }

  public ZonedDateTime getNativeStart() {
    return nativeStart;
  }

  public ZonedDateTime getNormalizedStart() {
    return normalizedStart;
  }

  public ZonedDateTime getUtcStart() {
    return utcStart;
  }

  public ZonedDateTime getColumnStart() {
    return columnStart;
  }
}
