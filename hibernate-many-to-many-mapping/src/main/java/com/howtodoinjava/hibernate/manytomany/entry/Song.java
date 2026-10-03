package com.howtodoinjava.hibernate.manytomany.entry;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Song {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  @OneToMany(mappedBy = "song")
  private List<PlaylistSong> entries = new ArrayList<>();

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

  public List<PlaylistSong> getEntries() {
    return entries;
  }

  @Override
  public String toString() {
    return title;
  }
}
