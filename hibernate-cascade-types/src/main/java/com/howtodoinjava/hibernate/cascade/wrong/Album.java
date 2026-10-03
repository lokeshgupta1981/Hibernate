package com.howtodoinjava.hibernate.cascade.wrong;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Album {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  @OneToMany(mappedBy = "album")
  private List<Photo> photos = new ArrayList<>();

  protected Album() {
  }

  public Album(String title) {
    this.title = title;
  }

  public void addPhoto(Photo photo) {
    photos.add(photo);
    photo.setAlbum(this);
  }

  public Long getId() {
    return id;
  }

  public List<Photo> getPhotos() {
    return photos;
  }
}
