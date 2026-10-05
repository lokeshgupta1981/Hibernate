package com.howtodoinjava.hibernate.savechild.unidirectional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Question {

  @Id
  @GeneratedValue
  private Long id;

  private String text;

  private int position;

  protected Question() {
  }

  public Question(String text, int position) {
    this.text = text;
    this.position = position;
  }

  public Long getId() {
    return id;
  }
}
