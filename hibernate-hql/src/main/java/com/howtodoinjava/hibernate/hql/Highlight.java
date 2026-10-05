package com.howtodoinjava.hibernate.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Target of the insert ... select example: the artworks of the highlights tour. */
@Entity
@Table(name = "highlights")
public class Highlight {

  @Id
  @GeneratedValue
  private Long id;

  private String title;
  private String artistName;

  protected Highlight() {
  }

  public String getTitle() {
    return title;
  }

  public String getArtistName() {
    return artistName;
  }

  @Override
  public String toString() {
    return title + " by " + artistName;
  }
}
