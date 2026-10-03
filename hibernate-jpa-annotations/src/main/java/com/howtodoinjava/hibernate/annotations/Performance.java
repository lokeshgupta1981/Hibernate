package com.howtodoinjava.hibernate.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "Show")
@Table(name = "performance",
    uniqueConstraints = @UniqueConstraint(name = "uk_hall_time",
        columnNames = {"theater_id", "hall", "starts_at"}),
    indexes = @Index(name = "idx_starts_at", columnList = "starts_at"))
public class Performance {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "starts_at", nullable = false)
  private LocalDateTime startsAt;

  @Column(length = 30)
  private String hall;

  @Enumerated
  private Status status = Status.SCHEDULED;

  @Enumerated(EnumType.STRING)
  @Column(length = 2)
  private Language language = Language.ENGLISH;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "play_id")
  private Play play;

  @Column(name = "play_id", insertable = false, updatable = false)
  private Long playId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "theater_id")
  private Theater theater;

  protected Performance() {
  }

  public Performance(Play play, Theater theater, LocalDateTime startsAt, String hall) {
    this.play = play;
    this.theater = theater;
    this.startsAt = startsAt;
    this.hall = hall;
  }

  public UUID getId() {
    return id;
  }

  public LocalDateTime getStartsAt() {
    return startsAt;
  }

  public String getHall() {
    return hall;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public Play getPlay() {
    return play;
  }

  public Long getPlayId() {
    return playId;
  }

  public Theater getTheater() {
    return theater;
  }
}
