package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.type.YesNoConverter;

/** Same table as Bookmark, mapped another way. Used in its own database. */
@Entity(name = "Bookmark")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, converter = YesNoConverter.class)
public class TimestampConverterBookmark {

  @Id
  @GeneratedValue
  private Long id;

  private String title;
  private String url;

  protected TimestampConverterBookmark() {
  }

  public TimestampConverterBookmark(String title, String url) {
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
