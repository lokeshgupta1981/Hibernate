package com.howtodoinjava.hibernate.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "galleries")
public class Gallery {

  @Id
  @GeneratedValue
  private Long id;

  private String name;
  private Integer floor;

  @OneToMany(mappedBy = "gallery")
  private List<Artwork> artworks = new ArrayList<>();

  protected Gallery() {
  }

  public Gallery(String name, Integer floor) {
    this.name = name;
    this.floor = floor;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Integer getFloor() {
    return floor;
  }

  public List<Artwork> getArtworks() {
    return artworks;
  }

  @Override
  public String toString() {
    return name;
  }
}
