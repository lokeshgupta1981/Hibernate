package com.howtodoinjava.hibernate.namedqueryerrors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "volunteer_shift")
@NamedQuery(name = "Shift.findLong",
    query = "select s from Shift s where s.hours >= :hours order by s.startsAt")
@NamedQuery(name = "Shift.findByVolunteer",
    query = "select s from Shift s where s.volunteer.name = :name order by s.startsAt")
@NamedQuery(name = "Shift.findAfter",
    query = "select s from Shift s where s.startsAt > :from order by s.startsAt")
@NamedNativeQuery(name = "Shift.findLongSql",
    query = "select * from volunteer_shift where hours >= :hours order by starts_at",
    resultClass = Shift.class)
public class Shift {

  @Id
  @GeneratedValue
  private Long id;

  private String eventName;

  @Column(name = "starts_at")
  private LocalDateTime startsAt;

  private int hours;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "volunteer_id")
  private Volunteer volunteer;

  protected Shift() {
  }

  public Shift(String eventName, LocalDateTime startsAt, int hours, Volunteer volunteer) {
    this.eventName = eventName;
    this.startsAt = startsAt;
    this.hours = hours;
    this.volunteer = volunteer;
  }

  public Long getId() {
    return id;
  }

  public String getEventName() {
    return eventName;
  }

  public LocalDateTime getStartsAt() {
    return startsAt;
  }

  public int getHours() {
    return hours;
  }

  public Volunteer getVolunteer() {
    return volunteer;
  }

  @Override
  public String toString() {
    return eventName + " (" + hours + "h)";
  }
}
