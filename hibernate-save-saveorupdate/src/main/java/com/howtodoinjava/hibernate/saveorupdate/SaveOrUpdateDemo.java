package com.howtodoinjava.hibernate.saveorupdate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.function.Consumer;

/** Runs persist() and merge() on Hibernate 7.4 for each entity state and prints the SQL at the call and at commit. */
public class SaveOrUpdateDemo {

  private static EntityManagerFactory emf;

  public static void main(String[] args) {
    emf = Database.create(false);
    try {
      Plant monstera = new Plant("Monstera", "Monstera deliciosa", "medium", 25.00);
      emf.runInTransaction(em -> em.persist(monstera));
      Long id = monstera.getId();
      System.out.println("Seed: " + monstera);

      // 1. New plant
      Plant basil = new Plant("Basil", "Ocimum basilicum", "small", 4.00);
      step("1. persist() a new plant", em -> {
        em.persist(basil);
        System.out.println("   basil.getId()=" + basil.getId() + ", contains=" + em.contains(basil));
        printSql("at the call");
      });

      Plant fern = new Plant("Fern", "Nephrolepis exaltata", "medium", 9.00);
      step("2. merge() a new plant", em -> {
        Plant copy = em.merge(fern);
        System.out.println("   copy.getId()=" + copy.getId() + ", fern.getId()=" + fern.getId()
            + ", copy == fern: " + (copy == fern) + ", contains(fern)=" + em.contains(fern));
        printSql("at the call");
      });

      // 2. Managed plant
      step("3. persist() a managed plant", em -> {
        Plant managed = em.find(Plant.class, id);
        SqlLog.clear();
        managed.setPrice(27.00);
        em.persist(managed);
        printSql("at the call");
      });

      step("4. merge() a managed plant", em -> {
        Plant managed = em.find(Plant.class, id);
        SqlLog.clear();
        Plant result = em.merge(managed);
        System.out.println("   result == managed: " + (result == managed));
        printSql("at the call");
      });

      // 3. Detached plant
      Plant detached = emf.callInTransaction(em -> em.find(Plant.class, id));
      tryStep("5. persist() a detached plant", em -> em.persist(detached));

      Plant unchanged = emf.callInTransaction(em -> em.find(Plant.class, id));
      step("6. merge() a detached plant, no change", em -> {
        em.merge(unchanged);
        printSql("at the call");
      });

      Plant repotted = emf.callInTransaction(em -> em.find(Plant.class, id));
      repotted.setPotSize("large");
      step("7. merge() a detached plant, changed", em -> {
        Plant copy = em.merge(repotted);
        System.out.println("   copy == repotted: " + (copy == repotted) + ", contains(repotted)="
            + em.contains(repotted) + ", contains(copy)=" + em.contains(copy));
        printSql("at the call");
      });

      Plant priced = emf.callInTransaction(em -> em.find(Plant.class, id));
      priced.setPrice(30.00);
      step("8. merge() when the same id is already loaded", em -> {
        Plant loaded = em.find(Plant.class, id);
        SqlLog.clear();
        Plant copy = em.merge(priced);
        System.out.println("   copy == loaded: " + (copy == loaded) + ", loaded.getPrice()=" + loaded.getPrice());
        printSql("at the call");
      });

      // 4. Id that does not exist
      Plant cactus = new Plant("Cactus", "Echinopsis", "small", 6.00);
      cactus.setId(99L);
      tryStep("9. persist() a plant with id 99", em -> em.persist(cactus));
      tryStep("10. merge() a plant with id 99", em -> em.merge(cactus));

      // 5. saveOrUpdate() replacement
      Plant aloe = new Plant("Aloe", "Aloe vera", "small", 7.50);
      step("11. PlantStore.save() a new plant", em -> {
        Plant saved = PlantStore.save(em, aloe);
        System.out.println("   saved == aloe: " + (saved == aloe));
        printSql("at the call");
      });
      aloe.setPrice(8.00);
      step("12. PlantStore.save() the detached aloe", em -> {
        Plant saved = PlantStore.save(em, aloe);
        System.out.println("   saved == aloe: " + (saved == aloe));
        printSql("at the call");
      });

      System.out.println();
      System.out.println("Rows: " + Database.count(emf));
      emf.runInTransaction(em -> em.createQuery("from Plant order by id", Plant.class)
          .getResultList().forEach(p -> System.out.println("   " + p)));
    } finally {
      emf.close();
    }
  }

  private static void step(String title, Consumer<EntityManager> work) {
    System.out.println();
    System.out.println(title);
    SqlLog.clear();
    try (EntityManager em = emf.createEntityManager()) {
      em.getTransaction().begin();
      try {
        work.accept(em);
        SqlLog.clear();
        em.getTransaction().commit();
        printSql("at commit");
      } finally {
        if (em.getTransaction().isActive()) {
          em.getTransaction().rollback();
        }
      }
    }
  }

  private static void tryStep(String title, Consumer<EntityManager> work) {
    try {
      step(title, work);
    } catch (RuntimeException e) {
      printSql("before the error");
      System.out.println("   " + e.getClass().getName() + ": " + e.getMessage());
    }
  }

  private static void printSql(String when) {
    if (SqlLog.statements().isEmpty()) {
      System.out.println("   " + when + ": no SQL");
    }
    SqlLog.statements().forEach(sql -> System.out.println("   " + when + ": " + sql));
  }
}
