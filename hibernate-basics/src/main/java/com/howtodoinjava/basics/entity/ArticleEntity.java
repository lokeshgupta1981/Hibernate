package com.howtodoinjava.basics.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "TBL_ARTICLE")
public class ArticleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column
  private String title;

  @Column
  private String content;

  @Column
  private LocalDate lastUpdateDate;

  @Column
  private LocalTime lastUpdateTime;

  @Column
  private LocalDateTime publishedTimestamp;

  public Long getId() {return id;}

  public void setId(Long id) {this.id = id;}

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public LocalDate getLastUpdateDate() {
    return lastUpdateDate;
  }

  public void setLastUpdateDate(LocalDate lastUpdateDate) {
    this.lastUpdateDate = lastUpdateDate;
  }

  public LocalTime getLastUpdateTime() {
    return lastUpdateTime;
  }

  public void setLastUpdateTime(LocalTime lastUpdateTime) {
    this.lastUpdateTime = lastUpdateTime;
  }

  public LocalDateTime getPublishedTimestamp() {
    return publishedTimestamp;
  }

  public void setPublishedTimestamp(LocalDateTime publishedTimestamp) {
    this.publishedTimestamp = publishedTimestamp;
  }
}
