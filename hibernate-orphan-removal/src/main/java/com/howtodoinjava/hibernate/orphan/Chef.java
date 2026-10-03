package com.howtodoinjava.hibernate.orphan;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

// A chef's recipes can exist without the chef, so there is NO orphanRemoval here
@Entity
public class Chef {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @OneToMany(mappedBy = "chef")
  private List<Recipe> recipes = new ArrayList<>();

  protected Chef() {
  }

  public Chef(String name) {
    this.name = name;
  }

  public void addRecipe(Recipe recipe) {
    recipes.add(recipe);
    recipe.setChef(this);
  }

  public void removeRecipe(Recipe recipe) {
    recipes.remove(recipe);
    recipe.setChef(null);
  }

  public Long getId() {
    return id;
  }

  public List<Recipe> getRecipes() {
    return recipes;
  }
}
