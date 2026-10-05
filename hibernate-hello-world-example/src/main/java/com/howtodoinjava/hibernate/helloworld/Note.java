package com.howtodoinjava.hibernate.helloworld;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class Note {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String title;

  private String text;

  @Column(name = "created_on")
  private LocalDate createdOn;

  protected Note() {
  }

  public Note(String title, String text, LocalDate createdOn) {
    this.title = title;
    this.text = text;
    this.createdOn = createdOn;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public LocalDate getCreatedOn() {
    return createdOn;
  }

  @Override
  public String toString() {
    return "Note[id=" + id + ", title=" + title + ", text=" + text + ", createdOn=" + createdOn + "]";
  }
}
