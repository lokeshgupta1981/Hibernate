package com.howtodoinjava.hibernate.datetime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class Webinar {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  private LocalDate eventDate;                  // date
  private LocalTime startTime;                  // time(0)
  private LocalDateTime registrationCloses;     // timestamp(6)
  private Instant startsAt;                     // timestamp(6) with time zone
  private OffsetDateTime startWithOffset;       // timestamp(6) with time zone
  private ZonedDateTime startInZone;            // timestamp(6) with time zone
  private ZoneId hostZone;                      // varchar(255)
  private Duration length;                      // numeric(21,0)

  @CreationTimestamp
  private Instant createdOn;

  @UpdateTimestamp
  private Instant updatedOn;

  protected Webinar() {
  }

  /** Builds every field from one start time in the host's zone. */
  public Webinar(String title, ZonedDateTime start, Duration length) {
    this.title = title;
    this.eventDate = start.toLocalDate();
    this.startTime = start.toLocalTime();
    this.registrationCloses = start.toLocalDateTime().minusDays(1);
    this.startsAt = start.toInstant();
    this.startWithOffset = start.toOffsetDateTime();
    this.startInZone = start;
    this.hostZone = start.getZone();
    this.length = length;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getRegistrationCloses() {
    return registrationCloses;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public OffsetDateTime getStartWithOffset() {
    return startWithOffset;
  }

  public ZonedDateTime getStartInZone() {
    return startInZone;
  }

  public ZoneId getHostZone() {
    return hostZone;
  }

  public Duration getLength() {
    return length;
  }

  public Instant getCreatedOn() {
    return createdOn;
  }

  public Instant getUpdatedOn() {
    return updatedOn;
  }

  @Override
  public String toString() {
    return title;
  }
}
