package com.howtodoinjava.hibernate.hql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity
@Table(name = "artworks")
public class Artwork {

  @Id
  @GeneratedValue
  private Long id;

  private String title;
  @Column(name = "created_year")
  private Integer year;
  private BigDecimal estimatedValue;

  @Enumerated(EnumType.STRING)
  private Medium medium;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "artist_id")
  private Artist artist;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "gallery_id")
  private Gallery gallery;      // null = in storage

  protected Artwork() {
  }

  public Artwork(String title, Integer year, BigDecimal estimatedValue, Medium medium,
      Artist artist, Gallery gallery) {
    this.title = title;
    this.year = year;
    this.estimatedValue = estimatedValue;
    this.medium = medium;
    this.artist = artist;
    this.gallery = gallery;
    artist.getArtworks().add(this);
    if (gallery != null) {
      gallery.getArtworks().add(this);
    }
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public Integer getYear() {
    return year;
  }

  public BigDecimal getEstimatedValue() {
    return estimatedValue;
  }

  public Medium getMedium() {
    return medium;
  }

  public Artist getArtist() {
    return artist;
  }

  public Gallery getGallery() {
    return gallery;
  }

  @Override
  public String toString() {
    return title;
  }
}
