package com.howtodoinjava.hibernate.manytomany.bag;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Song {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  @ManyToMany(mappedBy = "songs")
  private List<Playlist> playlists = new ArrayList<>();

  protected Song() {
  }

  public Song(String title) {
    this.title = title;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public List<Playlist> getPlaylists() {
    return playlists;
  }
}
