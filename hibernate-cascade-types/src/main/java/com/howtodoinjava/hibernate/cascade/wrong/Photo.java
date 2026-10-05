package com.howtodoinjava.hibernate.cascade.wrong;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import java.util.HashSet;
import java.util.Set;

/** Wrong mappings on purpose: REMOVE cascades from the child to shared entities. */
@Entity
public class Photo {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)   // wrong
  @JoinColumn(name = "album_id")
  private Album album;

  @ManyToMany(cascade = CascadeType.REMOVE)                          // wrong
  @JoinTable(name = "photo_person",
      joinColumns = @JoinColumn(name = "photo_id"),
      inverseJoinColumns = @JoinColumn(name = "person_id"))
  private Set<Person> people = new HashSet<>();

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

  void setAlbum(Album album) {
    this.album = album;
  }

  public Set<Person> getPeople() {
    return people;
  }
}
