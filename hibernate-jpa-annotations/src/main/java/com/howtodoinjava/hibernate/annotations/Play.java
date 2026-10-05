package com.howtodoinjava.hibernate.annotations;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
@Table(name = "play",
    indexes = @Index(name = "idx_play_genre", columnList = "genre"),
    comment = "Plays in the repertoire")
public class Play {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "play_seq")
  @SequenceGenerator(name = "play_seq", sequenceName = "play_seq", allocationSize = 10)
  private Long id;

  @Column(nullable = false, length = 120, unique = true)
  private String title;

  @Enumerated(EnumType.STRING)
  private Genre genre;

  @Column(name = "duration_minutes",
      check = @CheckConstraint(name = "ck_duration", constraint = "duration_minutes > 0"),
      comment = "Running time in minutes")
  private int durationMinutes;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  private String synopsis;

  @Column(name = "ticket_price", precision = 6, scale = 2)
  private BigDecimal ticketPrice;

  @Convert(converter = YesNoConverter.class)
  @Column(length = 1)
  private boolean intermission;

  @Transient
  private boolean featured;

  @Version
  private Integer version;

  protected Play() {
  }

  public Play(String title, Genre genre, int durationMinutes, BigDecimal ticketPrice) {
    this.title = title;
    this.genre = genre;
    this.durationMinutes = durationMinutes;
    this.ticketPrice = ticketPrice;
  }

  @Access(AccessType.PROPERTY)
  @Column(length = 120)
  public String getSlug() {
    return title == null ? null : title.toLowerCase().replace(' ', '-');
  }

  public void setSlug(String slug) {
    // computed from the title; the stored value is not read back
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

  public Genre getGenre() {
    return genre;
  }

  public int getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(int durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public String getSynopsis() {
    return synopsis;
  }

  public void setSynopsis(String synopsis) {
    this.synopsis = synopsis;
  }

  public BigDecimal getTicketPrice() {
    return ticketPrice;
  }

  public void setTicketPrice(BigDecimal ticketPrice) {
    this.ticketPrice = ticketPrice;
  }

  public boolean isIntermission() {
    return intermission;
  }

  public void setIntermission(boolean intermission) {
    this.intermission = intermission;
  }

  public boolean isFeatured() {
    return featured;
  }

  public void setFeatured(boolean featured) {
    this.featured = featured;
  }

  public Integer getVersion() {
    return version;
  }
}
