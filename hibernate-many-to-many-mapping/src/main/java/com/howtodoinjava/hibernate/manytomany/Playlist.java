package com.howtodoinjava.hibernate.manytomany;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

/** Owning side: its @JoinTable decides what goes into playlist_song. */
@Entity
public class Playlist {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(name = "playlist_song",
      joinColumns = @JoinColumn(name = "playlist_id"),
      inverseJoinColumns = @JoinColumn(name = "song_id"))
  private Set<Song> songs = new HashSet<>();

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

  public String getName() {
    return name;
  }

  public Set<Song> getSongs() {
    return songs;
  }
}
