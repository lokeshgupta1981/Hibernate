package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** Test-only: the same table without @SoftDelete, for the hard delete SQL. */
@Entity(name = "Bookmark")
public class HardDeleteBookmark {

  @Id
  @GeneratedValue
  Long id;

  String title;

  protected HardDeleteBookmark() {
  }

  HardDeleteBookmark(String title) {
    this.title = title;
  }
}
