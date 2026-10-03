package com.howtodoinjava.hibernate.manytomany;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

/** Inverse side: mappedBy points to Playlist.songs, so changes here alone are not saved. */
@Entity
public class Song {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  @ManyToMany(mappedBy = "songs")
  private Set<Playlist> playlists = new HashSet<>();

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

  public Set<Playlist> getPlaylists() {
    return playlists;
  }

  @Override
  public String toString() {
    return title;
  }
}
