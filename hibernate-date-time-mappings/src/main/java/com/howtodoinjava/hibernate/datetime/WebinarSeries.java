package com.howtodoinjava.hibernate.datetime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.Date;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SourceType;
import org.hibernate.type.SqlTypes;

/** A series of webinars: the less common types, legacy types and the database clock. */
@Entity
public class WebinarSeries {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  private Year season;                          // integer
  private YearMonth launchMonth;                // varbinary(255), serialized
  @Convert(converter = YearMonthConverter.class)
  private YearMonth endMonth;                   // date, first day of the month

  private OffsetTime dailyStart;                // time(0) with time zone

  @Column(secondPrecision = 3)
  private LocalTime reminderTime;               // time(3)

  @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
  private Duration breakLength;                 // interval second(9,6)

  @Temporal(TemporalType.TIMESTAMP)
  private Date legacyCreated;                   // timestamp(6), deprecated mapping

  @Temporal(TemporalType.DATE)
  private Calendar legacyLaunchDay;             // date, deprecated mapping

  @CreationTimestamp(source = SourceType.DB)
  private Instant createdOn;

  protected WebinarSeries() {
  }

  public WebinarSeries(String name, Year season, YearMonth launchMonth, OffsetTime dailyStart,
      Duration breakLength) {
    this.name = name;
    this.season = season;
    this.launchMonth = launchMonth;
    this.dailyStart = dailyStart;
    this.breakLength = breakLength;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Year getSeason() {
    return season;
  }

  public YearMonth getLaunchMonth() {
    return launchMonth;
  }

  public YearMonth getEndMonth() {
    return endMonth;
  }

  public void setEndMonth(YearMonth endMonth) {
    this.endMonth = endMonth;
  }

  public OffsetTime getDailyStart() {
    return dailyStart;
  }

  public LocalTime getReminderTime() {
    return reminderTime;
  }

  public void setReminderTime(LocalTime reminderTime) {
    this.reminderTime = reminderTime;
  }

  public Duration getBreakLength() {
    return breakLength;
  }

  public Date getLegacyCreated() {
    return legacyCreated;
  }

  public void setLegacyCreated(Date legacyCreated) {
    this.legacyCreated = legacyCreated;
  }

  public Calendar getLegacyLaunchDay() {
    return legacyLaunchDay;
  }

  public void setLegacyLaunchDay(Calendar legacyLaunchDay) {
    this.legacyLaunchDay = legacyLaunchDay;
  }

  public Instant getCreatedOn() {
    return createdOn;
  }
}
