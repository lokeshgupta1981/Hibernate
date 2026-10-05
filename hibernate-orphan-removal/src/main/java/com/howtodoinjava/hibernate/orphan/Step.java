package com.howtodoinjava.hibernate.orphan;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Step {

  @Id
  @GeneratedValue
  private Long id;

  private String instruction;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipe_id")
  private Recipe recipe;

  protected Step() {
  }

  public Step(String instruction) {
    this.instruction = instruction;
  }

  public Long getId() {
    return id;
  }

  public String getInstruction() {
    return instruction;
  }

  void setRecipe(Recipe recipe) {
    this.recipe = recipe;
  }
}
