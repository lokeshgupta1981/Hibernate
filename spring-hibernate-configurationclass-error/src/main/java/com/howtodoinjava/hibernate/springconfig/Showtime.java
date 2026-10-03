package com.howtodoinjava.hibernate.springconfig;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Showtime {

  @Id
  @GeneratedValue
  private Long id;

  private String movieTitle;
  private int screen;
  private LocalDateTime startsAt;

  protected Showtime() {
  }

  public Showtime(String movieTitle, int screen, LocalDateTime startsAt) {
    this.movieTitle = movieTitle;
    this.screen = screen;
    this.startsAt = startsAt;
  }

  public Long getId() {
    return id;
  }

  public String getMovieTitle() {
    return movieTitle;
  }

  public int getScreen() {
    return screen;
  }

  public LocalDateTime getStartsAt() {
    return startsAt;
  }

  @Override
  public String toString() {
    return movieTitle + " (screen " + screen + ", " + startsAt + ")";
  }
}
