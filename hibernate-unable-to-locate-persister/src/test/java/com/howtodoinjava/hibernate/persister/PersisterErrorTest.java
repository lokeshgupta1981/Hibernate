package com.howtodoinjava.hibernate.persister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.howtodoinjava.hibernate.persister.dto.FerryRouteDto;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.query.sqm.UnknownEntityException;
import org.junit.jupiter.api.Test;

class PersisterErrorTest {

  private static final String ROUTE = "com.howtodoinjava.hibernate.persister.FerryRoute";

  private static FerryRoute helsinkiTallinn() {
    return new FerryRoute("Helsinki", "Tallinn", 120);
  }

  private static long countRoutes(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from FerryRoute", Long.class).getSingleResult());
  }

  // Cause 1: the class is not registered with the configuration

  @Test
  void persistOfUnregisteredEntityFails() {
    try (EntityManagerFactory emf = Database.create(false)) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.persist(helsinkiTallinn())));
      assertEquals("Unknown entity type '" + ROUTE + "' ('FerryRoute' does not belong to this persistence unit)",
          e.getMessage());
      assertInstanceOf(UnknownEntityTypeException.class, e.getCause());
    }
  }

  @Test
  void findOfUnregisteredEntityFails() {
    try (EntityManagerFactory emf = Database.create(false)) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.find(FerryRoute.class, 1L)));
      assertEquals("Unknown entity type '" + ROUTE + "'", e.getMessage());
      assertInstanceOf(UnknownEntityTypeException.class, e.getCause());
    }
  }

  @Test
  void queryOfUnregisteredEntityFails() {
    try (EntityManagerFactory emf = Database.create(false)) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.createQuery("from FerryRoute", FerryRoute.class)));
      assertEquals("org.hibernate.query.sqm.UnknownEntityException: Could not resolve root entity 'FerryRoute'",
          e.getMessage());
      assertInstanceOf(UnknownEntityException.class, e.getCause());
    }
  }

  @Test
  void criteriaOfUnregisteredEntityFails() {
    try (EntityManagerFactory emf = Database.create(false)) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em ->
              em.getCriteriaBuilder().createQuery(FerryRoute.class).from(FerryRoute.class)));
      assertEquals("Not an entity: " + ROUTE, e.getMessage());
    }
  }

  @Test
  void registeredEntityIsSaved() {
    try (EntityManagerFactory emf = Database.create(false, FerryRoute.class)) {
      emf.runInTransaction(em -> em.persist(helsinkiTallinn()));
      assertEquals(1, countRoutes(emf));
    }
  }

  // Cause 2: no @Entity on the class

  @Test
  void classWithoutEntityAnnotationIsSilentlyIgnored() {
    try (EntityManagerFactory emf = Database.create(false,
        com.howtodoinjava.hibernate.persister.missingentity.FerryRoute.class)) {
      assertEquals(0, emf.getMetamodel().getEntities().size());
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.persist(
              new com.howtodoinjava.hibernate.persister.missingentity.FerryRoute("Helsinki", "Tallinn", 120))));
      assertEquals("Unknown entity type 'com.howtodoinjava.hibernate.persister.missingentity.FerryRoute'"
          + " ('FerryRoute' is not annotated '@Entity')", e.getMessage());
    }
  }

  // Cause 3: javax.persistence.Entity instead of jakarta.persistence.Entity

  @Test
  void javaxEntityAnnotationIsIgnored() {
    try (EntityManagerFactory emf = Database.create(false,
        com.howtodoinjava.hibernate.persister.legacy.FerryRoute.class)) {
      assertEquals(0, emf.getMetamodel().getEntities().size());
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.persist(
              new com.howtodoinjava.hibernate.persister.legacy.FerryRoute("Helsinki", "Tallinn", 120))));
      assertEquals("Unknown entity type 'com.howtodoinjava.hibernate.persister.legacy.FerryRoute'"
          + " ('FerryRoute' is not annotated '@Entity')", e.getMessage());
    }
  }

  @Test
  void jakartaEntityAnnotationIsRead() {
    assertNotNull(FerryRoute.class.getAnnotation(jakarta.persistence.Entity.class));
    try (EntityManagerFactory emf = Database.create(false, FerryRoute.class)) {
      assertEquals("FerryRoute", emf.getMetamodel().entity(FerryRoute.class).getName());
    }
  }

  // persistence.xml

  @Test
  void unitWithoutClassElementFails() {
    try (EntityManagerFactory emf = Database.fromPersistenceXml("ferry-unlisted", false)) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.persist(helsinkiTallinn())));
      assertEquals("Unknown entity type '" + ROUTE + "' ('FerryRoute' does not belong to this persistence unit)",
          e.getMessage());
    }
  }

  @Test
  void unitWithClassElementWorks() {
    try (EntityManagerFactory emf = Database.fromPersistenceXml("ferry-listed", false)) {
      emf.runInTransaction(em -> em.persist(helsinkiTallinn()));
      assertEquals(1, countRoutes(emf));
    }
  }

  @Test
  void scannedUnitWorksWithScanJandex() {
    try (EntityManagerFactory emf = Database.fromPersistenceXml("ferry-scanned", false)) {
      emf.runInTransaction(em -> em.persist(helsinkiTallinn()));
      assertEquals(1, countRoutes(emf));
    }
  }

  // Cause 4: a DTO instead of the entity

  @Test
  void persistOfDtoFails() {
    try (EntityManagerFactory emf = Database.create(false, FerryRoute.class)) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.persist(new FerryRouteDto("Turku", "Stockholm", 660))));
      assertEquals("Unknown entity type 'com.howtodoinjava.hibernate.persister.dto.FerryRouteDto'"
          + " ('FerryRouteDto' is not annotated '@Entity')", e.getMessage());
    }
  }

  @Test
  void findWithDtoClassFails() {
    try (EntityManagerFactory emf = Database.create(false, FerryRoute.class)) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.find(FerryRouteDto.class, 1L)));
      assertEquals("Unknown entity type 'com.howtodoinjava.hibernate.persister.dto.FerryRouteDto'",
          e.getMessage());
    }
  }

  @Test
  void dtoMappedToEntityAndQueriedBack() {
    try (EntityManagerFactory emf = Database.create(false, FerryRoute.class)) {
      FerryRouteDto dto = new FerryRouteDto("Turku", "Stockholm", 660);
      Long id = emf.callInTransaction(em -> {
        FerryRoute route = new FerryRoute(dto.fromPort(), dto.toPort(), dto.durationMinutes());
        em.persist(route);
        return route.getId();
      });
      FerryRouteDto loaded = emf.callInTransaction(em -> em.createQuery(
              "select r.fromPort, r.toPort, r.durationMinutes from FerryRoute r where r.id = :id",
              FerryRouteDto.class)
          .setParameter("id", id).getSingleResult());
      assertEquals(dto, loaded);
    }
  }

  // Cause 5: the proxy class instead of the entity class

  @Test
  void findWithProxyClassFails() {
    try (EntityManagerFactory emf = Database.create(false, FerryRoute.class)) {
      Long id = emf.callInTransaction(em -> {
        FerryRoute route = helsinkiTallinn();
        em.persist(route);
        return route.getId();
      });
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> {
            FerryRoute ref = em.getReference(FerryRoute.class, id);
            assertEquals(ROUTE + "$HibernateProxy", ref.getClass().getName());
            em.find(ref.getClass(), id);
          }));
      assertEquals("Unknown entity type '" + ROUTE + "$HibernateProxy'", e.getMessage());
    }
  }

  @Test
  void findWithRealClassOfProxyWorks() {
    try (EntityManagerFactory emf = Database.create(false, FerryRoute.class)) {
      Long id = emf.callInTransaction(em -> {
        FerryRoute route = helsinkiTallinn();
        em.persist(route);
        return route.getId();
      });
      List<String> result = emf.callInTransaction(em -> {
        FerryRoute ref = em.getReference(FerryRoute.class, id);
        assertFalse(Hibernate.isInitialized(ref));
        Class<?> real = Hibernate.getClass(ref);
        assertTrue(Hibernate.isInitialized(ref));     // getClass() loaded the proxy
        Object found = em.find(real, id);
        return List.of(real.getName(), found.toString());
      });
      assertEquals(List.of(ROUTE, "Helsinki -> Tallinn (120 min)"), result);
    }
  }
}
