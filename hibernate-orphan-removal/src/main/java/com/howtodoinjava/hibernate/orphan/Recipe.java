package com.howtodoinjava.hibernate.orphan;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class Recipe {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  // Steps belong to one recipe only: orphan removal
  @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Step> steps = new ArrayList<>();

  // Nutrition facts belong to one recipe only: orphan removal
  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "nutrition_id")
  private Nutrition nutrition;

  // Tags are shared by many recipes: no orphan removal
  @ManyToMany
  @JoinTable(name = "recipe_tag",
      joinColumns = @JoinColumn(name = "recipe_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags = new HashSet<>();

  // A recipe can exist without a chef: no orphan removal on Chef.recipes
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chef_id")
  private Chef chef;

  protected Recipe() {
  }

  public Recipe(String name) {
    this.name = name;
  }

  public void addStep(Step step) {
    steps.add(step);
    step.setRecipe(this);
  }

  public void removeStep(Step step) {
    steps.remove(step);
    step.setRecipe(null);
  }

  public Step findStep(String instruction) {
    return steps.stream()
        .filter(s -> s.getInstruction().equals(instruction))
        .findFirst()
        .orElseThrow();
  }

  public void addTag(Tag tag) {
    tags.add(tag);
    tag.getRecipes().add(this);
  }

  public void removeTag(Tag tag) {
    tags.remove(tag);
    tag.getRecipes().remove(this);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<Step> getSteps() {
    return steps;
  }

  // Exists only to show the "collection was no longer referenced" error
  public void setSteps(List<Step> steps) {
    this.steps = steps;
  }

  public Nutrition getNutrition() {
    return nutrition;
  }

  public void setNutrition(Nutrition nutrition) {
    this.nutrition = nutrition;
  }

  public Set<Tag> getTags() {
    return tags;
  }

  void setChef(Chef chef) {
    this.chef = chef;
  }
}
