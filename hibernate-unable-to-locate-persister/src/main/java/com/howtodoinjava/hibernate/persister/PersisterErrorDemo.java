package com.howtodoinjava.hibernate.persister;

import com.howtodoinjava.hibernate.persister.dto.FerryRouteDto;
import jakarta.persistence.EntityManagerFactory;
import java.util.function.Consumer;
import org.hibernate.Hibernate;

public class PersisterErrorDemo {

  public static void main(String[] args) {

    step("1. FerryRoute is not registered with the configuration");
    try (EntityManagerFactory emf = Database.create(true)) {
      attempt(emf, em -> em.persist(new FerryRoute("Helsinki", "Tallinn", 120)));
      attempt(emf, em -> em.find(FerryRoute.class, 1L));
      attempt(emf, em -> em.createQuery("from FerryRoute", FerryRoute.class).getResultList());
    }

    step("1. Fix: managedClasses(FerryRoute.class)");
    try (EntityManagerFactory emf = Database.create(true, FerryRoute.class)) {
      attempt(emf, em -> em.persist(new FerryRoute("Helsinki", "Tallinn", 120)));
    }

    step("2. The class has no @Entity");
    try (EntityManagerFactory emf = Database.create(true,
        com.howtodoinjava.hibernate.persister.missingentity.FerryRoute.class)) {
      System.out.println("Entities in the metamodel: " + emf.getMetamodel().getEntities().size());
      attempt(emf, em -> em.persist(
          new com.howtodoinjava.hibernate.persister.missingentity.FerryRoute("Helsinki", "Tallinn", 120)));
    }

    step("3. persistence.xml: unit without <class>, unlisted classes excluded");
    try (EntityManagerFactory emf = Database.fromPersistenceXml("ferry-unlisted", true)) {
      attempt(emf, em -> em.persist(new FerryRoute("Helsinki", "Tallinn", 120)));
    }

    step("3. Fix: <class>com.howtodoinjava.hibernate.persister.FerryRoute</class>");
    try (EntityManagerFactory emf = Database.fromPersistenceXml("ferry-listed", true)) {
      attempt(emf, em -> em.persist(new FerryRoute("Helsinki", "Tallinn", 120)));
    }

    step("3. Fix: scanning with exclude-unlisted-classes=false and hibernate-scan-jandex");
    try (EntityManagerFactory emf = Database.fromPersistenceXml("ferry-scanned", true)) {
      attempt(emf, em -> em.persist(new FerryRoute("Helsinki", "Tallinn", 120)));
    }

    try (EntityManagerFactory emf = Database.create(true, FerryRoute.class)) {
      step("4. Passing a DTO instead of the entity");
      FerryRouteDto dto = new FerryRouteDto("Turku", "Stockholm", 660);
      attempt(emf, em -> em.persist(dto));
      attempt(emf, em -> em.find(FerryRouteDto.class, 1L));

      step("4. Fix: map the DTO to the entity, and query into the DTO");
      Long id = emf.callInTransaction(em -> {
        FerryRoute route = new FerryRoute(dto.fromPort(), dto.toPort(), dto.durationMinutes());
        em.persist(route);
        return route.getId();
      });
      emf.runInTransaction(em -> System.out.println(em.createQuery(
              "select r.fromPort, r.toPort, r.durationMinutes from FerryRoute r where r.id = :id",
              FerryRouteDto.class)
          .setParameter("id", id).getSingleResult()));

      step("5. Passing a proxy class to find()");
      attempt(emf, em -> {
        FerryRoute ref = em.getReference(FerryRoute.class, id);
        System.out.println("ref.getClass() = " + ref.getClass().getName());
        em.find(ref.getClass(), id);
      });

      step("5. Fix: Hibernate.getClass(ref)");
      attempt(emf, em -> {
        FerryRoute ref = em.getReference(FerryRoute.class, id);
        System.out.println("Hibernate.getClass(ref) = " + Hibernate.getClass(ref).getName());
        System.out.println(em.find(Hibernate.getClass(ref), id));
      });
    }
  }

  private static void attempt(EntityManagerFactory emf, Consumer<jakarta.persistence.EntityManager> work) {
    try {
      emf.runInTransaction(work);
      System.out.println("OK");
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
      if (e.getCause() != null) {
        System.out.println("  caused by " + e.getCause().getClass().getName());
      }
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("--- " + title);
  }
}
