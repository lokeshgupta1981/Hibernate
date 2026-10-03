package com.howtodoinjava.hibernate.sfimplementor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Fixture {

  @Id
  @GeneratedValue
  private Long id;

  private String homeTeam;
  private String awayTeam;
  private LocalDateTime kickoff;
  private String venue;

  protected Fixture() {
  }

  public Fixture(String homeTeam, String awayTeam, LocalDateTime kickoff, String venue) {
    this.homeTeam = homeTeam;
    this.awayTeam = awayTeam;
    this.kickoff = kickoff;
    this.venue = venue;
  }

  public Long getId() {
    return id;
  }

  public String getHomeTeam() {
    return homeTeam;
  }

  public String getAwayTeam() {
    return awayTeam;
  }

  public LocalDateTime getKickoff() {
    return kickoff;
  }

  public String getVenue() {
    return venue;
  }

  @Override
  public String toString() {
    return homeTeam + " vs " + awayTeam + " at " + venue + ", " + kickoff;
  }
}
