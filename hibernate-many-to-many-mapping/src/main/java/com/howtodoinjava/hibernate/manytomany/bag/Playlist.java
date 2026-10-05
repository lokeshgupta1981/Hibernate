package com.howtodoinjava.hibernate.manytomany.bag;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import java.util.ArrayList;
import java.util.List;

/**
 * The mapping to avoid: a List (a "bag" for Hibernate), CascadeType.ALL and no @JoinTable
 * (default names Playlist_Song, playlists_id, songs_id).
 */
@Entity
public class Playlist {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @ManyToMany(cascade = CascadeType.ALL)
  private List<Song> songs = new ArrayList<>();

  protected Playlist() {
  }

  public Playlist(String name) {
    this.name = name;
  }

  public void addSong(Song song) {
    songs.add(song);
    song.getPlaylists().add(this);
  }

  public void removeSong(Song song) {
    songs.remove(song);
    song.getPlaylists().remove(this);
  }

  public Song findSong(String title) {
    return songs.stream().filter(s -> s.getTitle().equals(title)).findFirst().orElseThrow();
  }

  public Long getId() {
    return id;
  }

  public List<Song> getSongs() {
    return songs;
  }
}
