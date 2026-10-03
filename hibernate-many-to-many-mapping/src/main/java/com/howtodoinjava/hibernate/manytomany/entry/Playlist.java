package com.howtodoinjava.hibernate.manytomany.entry;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Playlist {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position")
  private List<PlaylistSong> entries = new ArrayList<>();

  protected Playlist() {
  }

  public Playlist(String name) {
    this.name = name;
  }

  public void addSong(Song song, int position, LocalDate addedOn) {
    PlaylistSong entry = new PlaylistSong(this, song, position, addedOn);
    entries.add(entry);
    song.getEntries().add(entry);
  }

  public void removeSong(Song song) {
    PlaylistSong entry = entries.stream()
        .filter(e -> e.getSong().equals(song))
        .findFirst()
        .orElseThrow();
    entries.remove(entry);
    song.getEntries().remove(entry);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<PlaylistSong> getEntries() {
    return entries;
  }
}
