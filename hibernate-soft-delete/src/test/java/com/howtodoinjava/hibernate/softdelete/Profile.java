package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.SoftDelete;

/** Test-only: a browser profile that shares bookmarks through a soft-deleted join table. */
@Entity
public class Profile {

  @Id
  @GeneratedValue
  Long id;

  String name;

  @ManyToMany
  @JoinTable(name = "profile_bookmark",
      joinColumns = @JoinColumn(name = "profile_id"),
      inverseJoinColumns = @JoinColumn(name = "bookmark_id"))
  @SoftDelete
  Set<Bookmark> bookmarks = new HashSet<>();

  protected Profile() {
  }

  Profile(String name) {
    this.name = name;
  }
}
