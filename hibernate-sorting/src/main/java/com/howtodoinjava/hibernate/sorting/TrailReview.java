package com.howtodoinjava.hibernate.sorting;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class TrailReview {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String author;
  private int rating;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trail_id")
  private Trail trail;

  protected TrailReview() {
  }

  public TrailReview(String author, int rating) {
    this.author = author;
    this.rating = rating;
  }

  public Long getId() {
    return id;
  }

  public String getAuthor() {
    return author;
  }

  public int getRating() {
    return rating;
  }

  public Trail getTrail() {
    return trail;
  }

  void setTrail(Trail trail) {
    this.trail = trail;
  }

  @Override
  public String toString() {
    return author + " " + rating;
  }
}
