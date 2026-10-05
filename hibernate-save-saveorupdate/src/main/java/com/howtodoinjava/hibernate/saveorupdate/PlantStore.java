package com.howtodoinjava.hibernate.saveorupdate;

import jakarta.persistence.EntityManager;

/**
 * A saveOrUpdate() replacement with the same rule as Spring Data's SimpleJpaRepository.save():
 * a new entity (id is null) goes to persist(), any other entity goes to merge().
 */
public final class PlantStore {

  private PlantStore() {
  }

  public static Plant save(EntityManager em, Plant plant) {
    if (plant.getId() == null) {
      em.persist(plant);
      return plant;
    }
    return em.merge(plant);
  }
}
