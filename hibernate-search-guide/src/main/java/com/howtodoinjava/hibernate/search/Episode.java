package com.howtodoinjava.hibernate.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDate;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity
@Indexed
public class Episode {

  @Id
  @GeneratedValue
  private Long id;

  @FullTextField(analyzer = "english")
  @KeywordField(name = "title_sort", sortable = Sortable.YES, normalizer = "lowercase")
  private String title;

  @FullTextField(analyzer = "english", highlightable = Highlightable.UNIFIED)
  @Column(length = 2000)
  private String description;

  @KeywordField
  @KeywordField(name = "host_ignorecase", normalizer = "lowercase")
  private String host;

  @GenericField(sortable = Sortable.YES)
  private LocalDate publishedOn;

  @GenericField(sortable = Sortable.YES)
  private int durationMinutes;

  protected Episode() {
  }

  public Episode(String title, String description, String host, LocalDate publishedOn, int durationMinutes) {
    this.title = title;
    this.description = description;
    this.host = host;
    this.publishedOn = publishedOn;
    this.durationMinutes = durationMinutes;
  }

  public Long getId() { return id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public String getHost() { return host; }
  public LocalDate getPublishedOn() { return publishedOn; }
  public int getDurationMinutes() { return durationMinutes; }

  @Override
  public String toString() {
    return title;
  }
}
