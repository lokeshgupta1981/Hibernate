package com.howtodoinjava.hibernate.manytomany.entry;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Join entity with its own id: the same song may appear twice in one playlist. */
@Entity
@Table(name = "playlist_item")
public class PlaylistItem {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "playlist_id")
  private Playlist playlist;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "song_id")
  private Song song;

  private int position;

  protected PlaylistItem() {
  }

  public PlaylistItem(Playlist playlist, Song song, int position) {
    this.playlist = playlist;
    this.song = song;
    this.position = position;
  }

  public Long getId() {
    return id;
  }
}
