package com.howtodoinjava.hibernate.savechild.unidirectional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Survey {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  // Unidirectional: the parent owns the foreign key column in the question table
  @OneToMany(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "survey_id")
  private List<Question> questions = new ArrayList<>();

  protected Survey() {
  }

  public Survey(String title) {
    this.title = title;
  }

  public Long getId() {
    return id;
  }

  public List<Question> getQuestions() {
    return questions;
  }
}
