package com.howtodoinjava.hibernate.mapping;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.Supplier;

public class MappingDemo {

  public static void main(String[] args) {
    run("1. Annotations only", () -> Database.annotations(true), "bicycle",
        emf -> emf.callInTransaction(em -> {
          Bicycle city = new Bicycle("City Cruiser", 54, new BigDecimal("12.50"));
          em.persist(city);
          return city;
        }));

    run("2. orm.xml only (plain class, no annotations)", () -> Database.ormXml(true), "bicycle",
        emf -> emf.callInTransaction(em -> {
          var city = new com.howtodoinjava.hibernate.mapping.plain.Bicycle("City Cruiser", 54, new BigDecimal("12.50"));
          em.persist(city);
          return city;
        }));

    run("3. Annotations + orm.xml override (metadata-complete=false)", () -> Database.annotationsWithOverride(true), "rental_bicycle",
        emf -> emf.callInTransaction(em -> {
          Bicycle city = new Bicycle("City Cruiser", 54, new BigDecimal("12.50"));
          em.persist(city);
          return city;
        }));

    run("4. Annotations + orm.xml with metadata-complete=true", () -> Database.metadataComplete(true), "rental_bicycle",
        emf -> emf.callInTransaction(em -> {
          Bicycle city = new Bicycle("City Cruiser", 54, new BigDecimal("12.50"));
          em.persist(city);
          return city;
        }));

    run("5. Legacy hbm.xml (deprecated)", () -> Database.hbmXml(true), "bicycle",
        emf -> emf.callInTransaction(em -> {
          var city = new com.howtodoinjava.hibernate.mapping.plain.Bicycle("City Cruiser", 54, new BigDecimal("12.50"));
          em.persist(city);
          return city;
        }));
  }

  private static void run(String title, Supplier<EntityManagerFactory> factory, String table,
      Function<EntityManagerFactory, Object> work) {
    System.out.println();
    System.out.println("=== " + title + " ===");
    try (EntityManagerFactory emf = factory.get()) {
      Object saved = work.apply(emf);
      System.out.println("Saved: " + saved);
      System.out.println("Columns of " + table + ": " + Database.columns(emf, table));
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
    }
  }
}
