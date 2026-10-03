package com.howtodoinjava.hibernate.emf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

class EntityManagerBootstrapTest {

  private static void addMenu(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      em.persist(new Drink("Latte", "small", "3.50"));
      em.persist(new Drink("Cappuccino", "medium", "4.20"));
      em.persist(new Drink("Mocha", "large", "4.80"));
    });
  }

  private static List<Drink> menu(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("from Drink order by price", Drink.class).getResultList());
  }

  @Test
  void quickReference() {
    try (EntityManagerFactory minimal = new PersistenceConfiguration("coffee-shop")
        .managedClass(Drink.class)
        .property(PersistenceConfiguration.JDBC_URL, "jdbc:h2:mem:menu-jpa")
        .createEntityManagerFactory()) {
      assertTrue(minimal.isOpen());
    }
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("coffee-shop");
    emf.runInTransaction(em -> em.persist(new Drink("Latte", "small", "3.50")));
    assertEquals("[Latte (small, 3.50)]", menu(emf).toString());
    emf.close();
    assertFalse(emf.isOpen());
  }

  @Test
  void twoPersistenceUnitsSideBySide() {
    try (EntityManagerFactory shop = EntityManagerFactories.fromPersistenceXml();
        EntityManagerFactory container = EntityManagerFactories.fromPersistenceUnitInfo()) {
      addMenu(shop);
      container.runInTransaction(em -> em.persist(new Drink("Chai Latte", "medium", "4.10")));
      assertEquals(3, EntityManagerDemo.count(shop));
      assertEquals(1, EntityManagerDemo.count(container));
    }
  }

  @Test
  void persistenceXmlCreatesTheNamedUnit() {
    try (EntityManagerFactory emf = EntityManagerFactories.fromPersistenceXml()) {
      assertEquals("coffee-shop", emf.getName());
      assertEquals("jdbc:h2:mem:menu", EntityManagerFactories.jdbcUrl(emf));
      assertEquals(PersistenceUnitTransactionType.RESOURCE_LOCAL, emf.getTransactionType());
    }
  }

  @Test
  void runInTransactionAndCallInTransaction() {
    try (EntityManagerFactory emf = EntityManagerFactories.fromPersistenceXml()) {
      addMenu(emf);
      assertEquals("[Latte (small, 3.50), Cappuccino (medium, 4.20), Mocha (large, 4.80)]",
          menu(emf).toString());
    }
  }

  @Test
  void runInTransactionBeginsAndClosesTheEntityManager() {
    try (EntityManagerFactory emf = EntityManagerFactories.fromPersistenceXml()) {
      AtomicReference<EntityManager> used = new AtomicReference<>();
      AtomicBoolean activeInside = new AtomicBoolean();
      emf.runInTransaction(em -> {
        used.set(em);
        activeInside.set(em.getTransaction().isActive());
      });
      assertTrue(activeInside.get());
      assertFalse(used.get().isOpen());
    }
  }

  @Test
  void entitiesReturnedByCallInTransactionAreDetached() {
    try (EntityManagerFactory emf = EntityManagerFactories.fromPersistenceXml()) {
      addMenu(emf);
      Drink latte = menu(emf).get(0);
      latte.setPrice(new BigDecimal("9.99"));          // no EntityManager tracks it any more
      assertEquals(new BigDecimal("3.50"),
          emf.callInTransaction(em -> em.find(Drink.class, latte.getId()).getPrice()));
    }
  }

  @Test
  void manualTransactionCommitsTheChange() {
    try (EntityManagerFactory emf = EntityManagerFactories.fromPersistenceXml()) {
      addMenu(emf);
      Long latteId = menu(emf).get(0).getId();
      EntityManager outer;
      try (EntityManager em = emf.createEntityManager()) {
        outer = em;
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.find(Drink.class, latteId).setPrice(new BigDecimal("3.75"));
        tx.commit();
        assertTrue(em.isOpen());
      }
      assertFalse(outer.isOpen());
      assertEquals(new BigDecimal("3.75"),
          emf.callInTransaction(em -> em.find(Drink.class, latteId).getPrice()));
    }
  }

  @Test
  void exceptionInRunInTransactionRollsBack() {
    try (EntityManagerFactory emf = EntityManagerFactories.fromPersistenceXml()) {
      addMenu(emf);
      AtomicReference<EntityManager> used = new AtomicReference<>();
      IllegalStateException e = assertThrows(IllegalStateException.class, () ->
          emf.runInTransaction(em -> {
            used.set(em);
            em.persist(new Drink("Espresso", "small", "2.90"));
            throw new IllegalStateException("Out of beans");
          }));
      assertEquals("Out of beans", e.getMessage());
      assertEquals(3, EntityManagerDemo.count(emf));
      assertFalse(used.get().isOpen());
    }
  }

  @Test
  void eachEntityManagerHasItsOwnPersistenceContext() {
    try (EntityManagerFactory emf = EntityManagerFactories.fromPersistenceXml()) {
      addMenu(emf);
      Long latteId = menu(emf).get(0).getId();
      try (EntityManager first = emf.createEntityManager();
          EntityManager second = emf.createEntityManager()) {
        Drink a = first.find(Drink.class, latteId);
        assertSame(a, first.find(Drink.class, latteId));
        assertNotSame(a, second.find(Drink.class, latteId));
        assertNotSame(first, second);
      }
    }
  }

  @Test
  void closedEntityManagerAndFactoryRefuseWork() {
    EntityManagerFactory emf = EntityManagerFactories.fromPersistenceXml();
    EntityManager em = emf.createEntityManager();
    em.close();
    IllegalStateException emError = assertThrows(IllegalStateException.class,
        () -> em.find(Drink.class, 1L));
    assertEquals("Session/EntityManager is closed", emError.getMessage());

    emf.close();
    assertFalse(emf.isOpen());
    IllegalStateException emfError = assertThrows(IllegalStateException.class,
        emf::createEntityManager);
    assertEquals("EntityManagerFactory is closed", emfError.getMessage());
  }

  @Test
  void runtimePropertiesOverridePersistenceXml() {
    try (EntityManagerFactory emf = EntityManagerFactories.fromPersistenceXmlWithOverrides()) {
      assertEquals("jdbc:h2:mem:menu-test", EntityManagerFactories.jdbcUrl(emf));
      assertEquals("false", emf.getProperties().get("hibernate.show_sql"));
      assertEquals("****", emf.getProperties().get("jakarta.persistence.jdbc.user"));   // masked
      assertEquals("****", emf.getProperties().get("jakarta.persistence.jdbc.password"));
    }
  }

  @Test
  void persistenceConfigurationWithoutXml() {
    try (EntityManagerFactory emf = EntityManagerFactories.withPersistenceConfiguration()) {
      emf.runInTransaction(em -> em.persist(new Drink("Flat White", "small", "3.90")));
      assertEquals("jdbc:h2:mem:menu-jpa", EntityManagerFactories.jdbcUrl(emf));
      assertEquals("coffee-shop", emf.getName());
      assertEquals(1, EntityManagerDemo.count(emf));
      assertInstanceOf(SessionFactory.class, emf);
      assertSame(emf, emf.unwrap(SessionFactory.class));
      assertInstanceOf(Session.class, emf.createEntityManager());
    }
  }

  @Test
  void hibernatePersistenceConfigurationReturnsSessionFactory() {
    try (SessionFactory sf = EntityManagerFactories.withHibernatePersistenceConfiguration()) {
      sf.inTransaction(session -> session.persist(new Drink("Americano", "large", "3.20")));
      assertEquals("jdbc:h2:mem:menu-hibernate", EntityManagerFactories.jdbcUrl(sf));
      assertEquals(1, EntityManagerDemo.count(sf));
      assertEquals(5, sf.getProperties().get("hibernate.connection.pool_size"));
      assertInstanceOf(Session.class, sf.createEntityManager());
    }
  }

  @Test
  void persistenceUnitInfoBootstrapForFrameworks() {
    try (EntityManagerFactory emf = EntityManagerFactories.fromPersistenceUnitInfo()) {
      emf.runInTransaction(em -> em.persist(new Drink("Chai Latte", "medium", "4.10")));
      assertEquals("coffee-shop-container", emf.getName());
      assertEquals(1, EntityManagerDemo.count(emf));
    }
  }

  @Test
  void oneSharedFactoryManyThreads() {
    EntityManagerFactory shared = CoffeeShopDatabase.emf();
    try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
      for (String name : List.of("Latte", "Mocha", "Cortado", "Macchiato")) {
        pool.submit(() -> shared.runInTransaction(em -> em.persist(new Drink(name, "small", "3.60"))));
      }
    }
    assertEquals(4, EntityManagerDemo.count(shared));
    assertSame(shared, CoffeeShopDatabase.emf());
    assertEquals("jdbc:h2:mem:menu-app", EntityManagerFactories.jdbcUrl(shared));
  }

  @Test
  void unknownPersistenceUnitName() {
    PersistenceException e = assertThrows(PersistenceException.class,
        () -> Persistence.createEntityManagerFactory("coffee"));
    assertEquals("No Persistence provider for EntityManager named coffee", e.getMessage());
  }
}
