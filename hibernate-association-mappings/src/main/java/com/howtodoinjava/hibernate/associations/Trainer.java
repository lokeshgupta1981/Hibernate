package com.howtodoinjava.hibernate.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Trainer {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  // Unidirectional one-to-many without mappedBy or @JoinColumn:
  // Hibernate creates the Trainer_FitnessClass join table
  @OneToMany
  private List<FitnessClass> classes = new ArrayList<>();

  protected Trainer() {
  }

  public Trainer(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public List<FitnessClass> getClasses() {
    return classes;
  }
}
