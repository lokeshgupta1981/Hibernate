package com.howtodoinjava.hibernate.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "artists")
public class Artist {

  @Id
  @GeneratedValue
  private Long id;

  private String name;
  private String country;

  @OneToMany(mappedBy = "artist")
  private List<Artwork> artworks = new ArrayList<>();

  protected Artist() {
  }

  public Artist(String name, String country) {
    this.name = name;
    this.country = country;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCountry() {
    return country;
  }

  public List<Artwork> getArtworks() {
    return artworks;
  }

  @Override
  public String toString() {
    return name;
  }
}
