package com.howtodoinjava.hibernate.cascade.merge;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

/** Album with CascadeType.MERGE only. */
@Entity
public class Album {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  @OneToMany(mappedBy = "album", cascade = CascadeType.MERGE)
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

  public void removePhoto(Photo photo) {
    photos.remove(photo);
    photo.setAlbum(null);
  }

  public Photo findPhoto(String title) {
    return photos.stream().filter(p -> p.getTitle().equals(title)).findFirst().orElseThrow();
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

  public List<Photo> getPhotos() {
    return photos;
  }
}
