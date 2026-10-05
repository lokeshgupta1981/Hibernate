package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/** Same table as Bookmark, mapped another way. Used in its own database. */
@Entity(name = "Bookmark")
@SQLDelete(sql = "update Bookmark set deleted = true where id = ?")
@SQLRestriction("deleted = false")
public class LegacyBookmark {

  @Id
  @GeneratedValue
  private Long id;

  private String title;
  private String url;

  private boolean deleted;

  protected LegacyBookmark() {
  }

  public LegacyBookmark(String title, String url) {
    this.title = title;
    this.url = url;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public boolean isDeleted() {
    return deleted;
  }

  @Override
  public String toString() {
    return title;
  }
}
