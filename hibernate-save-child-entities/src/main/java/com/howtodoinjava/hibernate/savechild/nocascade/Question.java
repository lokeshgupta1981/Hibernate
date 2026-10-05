package com.howtodoinjava.hibernate.savechild.nocascade;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Question {

  @Id
  @GeneratedValue
  private Long id;

  private String text;

  private int position;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "survey_id")
  private Survey survey;

  protected Question() {
  }

  public Question(String text, int position) {
    this.text = text;
    this.position = position;
  }

  public Long getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public int getPosition() {
    return position;
  }

  public Survey getSurvey() {
    return survey;
  }

  public void setSurvey(Survey survey) {
    this.survey = survey;
  }
}
