package com.howtodoinjava.hibernate.orphan;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Tag {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @ManyToMany(mappedBy = "tags")
  private Set<Recipe> recipes = new HashSet<>();

  protected Tag() {
  }

  public Tag(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Set<Recipe> getRecipes() {
    return recipes;
  }
}
