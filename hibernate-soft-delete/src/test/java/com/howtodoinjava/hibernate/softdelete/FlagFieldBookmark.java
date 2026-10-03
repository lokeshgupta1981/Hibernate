package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SoftDelete;

/** Test-only: tries to map the soft-delete column as a read-only field. */
@Entity(name = "Bookmark")
@SoftDelete
public class FlagFieldBookmark {

  @Id
  @GeneratedValue
  Long id;

  String title;

  @Column(name = "deleted", insertable = false, updatable = false)
  boolean deleted;

  protected FlagFieldBookmark() {
  }

  FlagFieldBookmark(String title) {
    this.title = title;
  }
}
