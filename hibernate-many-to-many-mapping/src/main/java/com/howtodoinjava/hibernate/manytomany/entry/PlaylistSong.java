package com.howtodoinjava.hibernate.manytomany.entry;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** The join table as an entity, so the link can carry addedOn and position. */
@Entity
@Table(name = "playlist_song")
public class PlaylistSong {

  @EmbeddedId
  private PlaylistSongId id = new PlaylistSongId();   // @MapsId fills both values

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("playlistId")
  private Playlist playlist;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("songId")
  private Song song;

  @Column(name = "added_on")
  private LocalDate addedOn;
  private int position;

  protected PlaylistSong() {
  }

  public PlaylistSong(Playlist playlist, Song song, int position, LocalDate addedOn) {
    this.playlist = playlist;
    this.song = song;
    this.position = position;
    this.addedOn = addedOn;
  }

  public Playlist getPlaylist() {
    return playlist;
  }

  public Song getSong() {
    return song;
  }

  public LocalDate getAddedOn() {
    return addedOn;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }
}
