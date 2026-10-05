package com.howtodoinjava.hibernate.orphan;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;

public class OrphanRemovalDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Save a chef and a pancake recipe with three steps, nutrition facts and a tag");
      Long[] ids = emf.callInTransaction(em -> {
        Tag breakfast = new Tag("breakfast");
        em.persist(breakfast);

        Chef chef = new Chef("Lokesh");
        em.persist(chef);

        Recipe pancakes = new Recipe("Pancakes");
        pancakes.addStep(new Step("Whisk"));
        pancakes.addStep(new Step("Heat"));
        pancakes.addStep(new Step("Fry"));
        pancakes.setNutrition(new Nutrition(350));
        pancakes.addTag(breakfast);
        chef.addRecipe(pancakes);
        em.persist(pancakes);
        return new Long[] {pancakes.getId(), chef.getId()};
      });
      Long recipeId = ids[0];
      Long chefId = ids[1];
      counts(emf);

      step("2. Remove the 'Heat' step (orphanRemoval = true)");
      emf.runInTransaction(em -> {
        Recipe pancakes = em.find(Recipe.class, recipeId);
        pancakes.removeStep(pancakes.findStep("Heat"));
      });
      counts(emf);

      step("3. Remove the recipe from the chef (no orphanRemoval)");
      emf.runInTransaction(em -> {
        Chef chef = em.find(Chef.class, chefId);
        chef.removeRecipe(em.find(Recipe.class, recipeId));
      });
      counts(emf);

      step("4. Replace the nutrition facts");
      emf.runInTransaction(em -> em.find(Recipe.class, recipeId).setNutrition(new Nutrition(420)));
      counts(emf);

      step("5. Set the nutrition facts to null");
      emf.runInTransaction(em -> em.find(Recipe.class, recipeId).setNutrition(null));
      counts(emf);

      step("6. Replace the steps collection with a new list");
      try {
        emf.runInTransaction(em -> em.find(Recipe.class, recipeId).setSteps(new ArrayList<>()));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
      counts(emf);

      }
      counts(emf);

      step("7. Remove the 'breakfast' tag (many-to-many)");
      emf.runInTransaction(em -> {
        Recipe pancakes = em.find(Recipe.class, recipeId);
        pancakes.removeTag(pancakes.getTags().iterator().next());
      });
      counts(emf);

      step("8. Delete the recipe");
      emf.runInTransaction(em -> em.remove(em.find(Recipe.class, recipeId)));
      counts(emf);
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }

  private static void counts(EntityManagerFactory emf) {
    System.out.println("chefs=" + Database.count(emf, "Chef")
        + " recipes=" + Database.count(emf, "Recipe")
        + " steps=" + Database.count(emf, "Step")
        + " nutrition=" + Database.count(emf, "Nutrition")
        + " tags=" + Database.count(emf, "Tag"));
        + " tags=" + Database.count(emf, "Tag"));
  }
}
