package com.howtodoinjava.hibernate.orphan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrphanRemovalTest {

  private EntityManagerFactory emf;
  private Long recipeId;
  private Long chefId;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
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
    recipeId = ids[0];
    chefId = ids[1];
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private long count(String entity) {
    return Database.count(emf, entity);
  }

  private List<String> stepNames() {
    return emf.callInTransaction(em -> em
        .createQuery("select instruction from Step order by instruction", String.class)
        .getResultList());
  }

  @Test
  void removingStepDeletesTheRow() {
    emf.runInTransaction(em -> {
      Recipe pancakes = em.find(Recipe.class, recipeId);
      pancakes.removeStep(pancakes.findStep("Heat"));
    });
    assertEquals(List.of("Fry", "Whisk"), stepNames());
  }

  @Test
  void clearingStepsDeletesAllRows() {
    emf.runInTransaction(em -> em.find(Recipe.class, recipeId).getSteps().clear());
    assertEquals(0, count("Step"));
    assertEquals(1, count("Recipe"));
  }

  @Test
  void removingStepsInLoopDeletesAllRows() {
    emf.runInTransaction(em -> {
      Recipe pancakes = em.find(Recipe.class, recipeId);
      new ArrayList<>(pancakes.getSteps()).forEach(pancakes::removeStep);
    });
    assertEquals(0, count("Step"));
  }

  @Test
  void withoutOrphanRemovalTheRecipeStaysWithNullChef() {
    emf.runInTransaction(em -> {
      Chef chef = em.find(Chef.class, chefId);
      chef.removeRecipe(em.find(Recipe.class, recipeId));
    });
    assertEquals(1, count("Recipe"));
    long withoutChef = emf.callInTransaction(em -> em
        .createQuery("select count(*) from Recipe where chef is null", Long.class)
        .getSingleResult());
    assertEquals(1, withoutChef);
  }

  @Test
  void withoutOrphanRemovalClearOnMappedBySideRunsNoUpdate() {
    emf.runInTransaction(em -> em.find(Chef.class, chefId).getRecipes().clear());
    long withChef = emf.callInTransaction(em -> em
        .createQuery("select count(*) from Recipe where chef.id = :id", Long.class)
        .setParameter("id", chefId).getSingleResult());
    assertEquals(1, withChef);
  }

  @Test
  void settingNutritionToNullDeletesIt() {
    emf.runInTransaction(em -> em.find(Recipe.class, recipeId).setNutrition(null));
    assertEquals(0, count("Nutrition"));
  }

  @Test
  void replacingNutritionDeletesTheOldOne() {
    emf.runInTransaction(em -> em.find(Recipe.class, recipeId).setNutrition(new Nutrition(420)));
    int calories = emf.callInTransaction(em -> em
        .createQuery("select n.calories from Nutrition n", Integer.class).getSingleResult());
    assertEquals(420, calories);
    assertEquals(1, count("Nutrition"));
  }

  @Test
  void mergingDetachedRecipeAppliesOrphanRemoval() {
    Recipe detached = emf.callInTransaction(em -> em
        .createQuery("select r from Recipe r join fetch r.steps where r.id = :id", Recipe.class)
        .setParameter("id", recipeId).getSingleResult());
    detached.removeStep(detached.findStep("Heat"));
    emf.runInTransaction(em -> em.merge(detached));
    assertEquals(List.of("Fry", "Whisk"), stepNames());
  }
  }

  @Test
  void replacingTheCollectionFails() {
    RollbackException e = assertThrows(RollbackException.class, () ->
        emf.runInTransaction(em -> em.find(Recipe.class, recipeId).setSteps(new ArrayList<>())));
    assertTrue(e.getMessage().contains(
        "A collection with orphan deletion was no longer referenced by the owning entity instance"));
    assertEquals(3, count("Step"));
  }

  @Test
  void swappingStepsInsideTheManagedCollectionWorks() {
    emf.runInTransaction(em -> {
      Recipe pancakes = em.find(Recipe.class, recipeId);
      List<Step> newSteps = List.of(new Step("Mix"), new Step("Bake"));
      pancakes.getSteps().clear();
      newSteps.forEach(pancakes::addStep);
    });
    assertEquals(List.of("Bake", "Mix"), stepNames());
  }

  @Test
  void removingTagDeletesOnlyTheLink() {
    emf.runInTransaction(em -> {
      Recipe pancakes = em.find(Recipe.class, recipeId);
      pancakes.removeTag(pancakes.getTags().iterator().next());
    });
    assertEquals(1, count("Tag"));
    long links = emf.callInTransaction(em -> ((Number) em
        .createNativeQuery("select count(*) from recipe_tag").getSingleResult()).longValue());
    assertEquals(0, links);
  }
    assertEquals(0, links);
  }

  @Test
  void movingStepToAnotherRecipeUpdatesTheForeignKeyInHibernate() {
    Long waffleId = emf.callInTransaction(em -> {
      Recipe waffles = new Recipe("Waffles");
      em.persist(waffles);
      return waffles.getId();
    });
    emf.runInTransaction(em -> {
      Recipe pancakes = em.find(Recipe.class, recipeId);
      Recipe waffles = em.find(Recipe.class, waffleId);
      Step whisk = pancakes.findStep("Whisk");
      pancakes.removeStep(whisk);
      waffles.addStep(whisk);
    });
    assertEquals(3, count("Step"));
    long inWaffles = emf.callInTransaction(em -> em
        .createQuery("select count(*) from Step where recipe.id = :id", Long.class)
        .setParameter("id", waffleId).getSingleResult());
    assertEquals(1, inWaffles);
  }

  @Test
  void deletingTheRecipeDeletesStepsAndNutritionButNotTags() {
    emf.runInTransaction(em -> em.remove(em.find(Recipe.class, recipeId)));
    assertEquals(0, count("Recipe"));
    assertEquals(0, count("Step"));
    assertEquals(0, count("Nutrition"));
    assertEquals(1, count("Tag"));
    assertEquals(1, count("Chef"));
  }
  }

  @Test
  void bulkDeleteIgnoresOrphanRemovalAndCascade() {
    assertThrows(PersistenceException.class, () ->
        emf.runInTransaction(em -> em.createQuery("delete from Recipe").executeUpdate()));
    assertEquals(3, count("Step"));
  }
}
