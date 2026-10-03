package com.howtodoinjava.hibernate.savechild;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Survey {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  @OneToMany(mappedBy = "survey", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private List<Question> questions = new ArrayList<>();

  protected Survey() {
  }

  public Survey(String title) {
    this.title = title;
  }

  // Sets both sides of the association
  public void addQuestion(Question question) {
    questions.add(question);
    question.setSurvey(this);
  }

  public void removeQuestion(Question question) {
    questions.remove(question);
    question.setSurvey(null);
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

  public List<Question> getQuestions() {
    return questions;
  }
}
