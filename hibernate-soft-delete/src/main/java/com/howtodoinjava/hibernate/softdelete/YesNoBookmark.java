package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.YesNoConverter;

/** Same table as Bookmark, mapped another way. Used in its own database. */
@Entity(name = "Bookmark")
@SoftDelete(columnName = "removed", converter = YesNoConverter.class)
public class YesNoBookmark {

  @Id
  @GeneratedValue
  private Long id;

  private String title;
  private String url;

  protected YesNoBookmark() {
  }

  public YesNoBookmark(String title, String url) {
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
