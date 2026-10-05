package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

/** Same table as Bookmark, mapped another way. Used in its own database. */
@Entity(name = "Bookmark")
@SoftDelete(strategy = SoftDeleteType.ACTIVE)
public class ActiveBookmark {

  @Id
  @GeneratedValue
  private Long id;

  private String title;
  private String url;

  protected ActiveBookmark() {
  }

  public ActiveBookmark(String title, String url) {
    this.title = title;
    this.url = url;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return title;
  }
}
