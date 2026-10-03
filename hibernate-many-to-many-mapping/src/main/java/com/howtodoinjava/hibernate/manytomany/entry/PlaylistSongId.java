package com.howtodoinjava.hibernate.manytomany.entry;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/** Composite primary key of playlist_song: one row per playlist and song pair. */
@Embeddable
public class PlaylistSongId {

  private Long playlistId;
  private Long songId;

  protected PlaylistSongId() {
  }

  public PlaylistSongId(Long playlistId, Long songId) {
    this.playlistId = playlistId;
    this.songId = songId;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PlaylistSongId other
        && Objects.equals(playlistId, other.playlistId)
        && Objects.equals(songId, other.songId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(playlistId, songId);
  }
}
