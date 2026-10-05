package com.howtodoinjava.hibernate.cascade.detach;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Photo {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "album_id")
  private Album album;

  protected Photo() {
  }

  public Photo(String title) {
    this.title = title;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Album getAlbum() {
    return album;
  }

  public void setAlbum(Album album) {
    this.album = album;
  }
}
