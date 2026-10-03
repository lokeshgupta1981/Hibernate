package com.howtodoinjava.hibernate.firstlevelcache;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Movie {

  @Id
  @GeneratedValue
  private Long id;

  private String title;
  private int releaseYear;
  private double rating;

  protected Movie() {
  }

  public Movie(String title, int releaseYear, double rating) {
    this.title = title;
    this.releaseYear = releaseYear;
    this.rating = rating;
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

  public int getReleaseYear() {
    return releaseYear;
  }

  public double getRating() {
    return rating;
  }

  public void setRating(double rating) {
    this.rating = rating;
  }

  @Override
  public String toString() {
    return title + " (" + releaseYear + ", " + rating + ")";
  }
}
