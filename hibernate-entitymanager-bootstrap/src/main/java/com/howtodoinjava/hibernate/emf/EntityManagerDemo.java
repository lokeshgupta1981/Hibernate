package com.howtodoinjava.hibernate.emf;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.hibernate.SessionFactory;

public class EntityManagerDemo {

  public static void main(String[] args) {
    step("1. Persistence.createEntityManagerFactory(\"coffee-shop\") reads META-INF/persistence.xml");
    EntityManagerFactory emf = EntityManagerFactories.fromPersistenceXml();
    System.out.println("name = " + emf.getName() + ", url = " + EntityManagerFactories.jdbcUrl(emf)
        + ", transactionType = " + emf.getTransactionType());

    step("2. runInTransaction(): persist three drinks");
    emf.runInTransaction(em -> {
      em.persist(new Drink("Latte", "small", "3.50"));
      em.persist(new Drink("Cappuccino", "medium", "4.20"));
      em.persist(new Drink("Mocha", "large", "4.80"));
    });

    step("3. callInTransaction(): return the menu");
    List<Drink> menu = emf.callInTransaction(em ->
        em.createQuery("from Drink order by price", Drink.class).getResultList());
    System.out.println("menu = " + menu);

    step("4. Manual EntityManager and EntityTransaction");
    Long latteId = menu.get(0).getId();
    try (EntityManager em = emf.createEntityManager()) {
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      Drink latte = em.find(Drink.class, latteId);
      latte.setPrice(new BigDecimal("3.75"));
      tx.commit();
      System.out.println("latte = " + latte + ", em.isOpen() = " + em.isOpen());
    }

    step("5. Exception inside runInTransaction() rolls back");
    try {
      emf.runInTransaction(em -> {
        em.persist(new Drink("Espresso", "small", "2.90"));
        throw new IllegalStateException("Out of beans");
      });
    } catch (IllegalStateException e) {
      System.out.println("caught: " + e.getMessage());
    }
    System.out.println("count = " + count(emf));

    step("6. One EntityManager per unit of work");
    try (EntityManager first = emf.createEntityManager();
        EntityManager second = emf.createEntityManager()) {
      Drink a = first.find(Drink.class, latteId);
      Drink b = first.find(Drink.class, latteId);
      Drink c = second.find(Drink.class, latteId);
      System.out.println("same EntityManager: a == b -> " + (a == b));
      System.out.println("two EntityManagers: a == c -> " + (a == c));
    }

    step("7. Close the factory");
    emf.close();
    System.out.println("emf.isOpen() = " + emf.isOpen());
    try {
      emf.createEntityManager();
    } catch (IllegalStateException e) {
      System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    step("8. persistence.xml with runtime overrides");
    try (EntityManagerFactory test = EntityManagerFactories.fromPersistenceXmlWithOverrides()) {
      System.out.println("url = " + EntityManagerFactories.jdbcUrl(test)
          + ", show_sql = " + test.getProperties().get("hibernate.show_sql"));
    }

    step("9. PersistenceConfiguration (Jakarta Persistence 3.2, no XML)");
    try (EntityManagerFactory jpa = EntityManagerFactories.withPersistenceConfiguration()) {
      jpa.runInTransaction(em -> em.persist(new Drink("Flat White", "small", "3.90")));
      System.out.println("url = " + EntityManagerFactories.jdbcUrl(jpa) + ", count = " + count(jpa)
          + ", is SessionFactory = " + (jpa instanceof SessionFactory));
    }

    step("10. HibernatePersistenceConfiguration");
    try (SessionFactory sf = EntityManagerFactories.withHibernatePersistenceConfiguration()) {
      sf.inTransaction(session -> session.persist(new Drink("Americano", "large", "3.20")));
      System.out.println("url = " + EntityManagerFactories.jdbcUrl(sf) + ", count = " + count(sf));
    }

    step("11. PersistenceUnitInfo + createContainerEntityManagerFactory()");
    try (EntityManagerFactory container = EntityManagerFactories.fromPersistenceUnitInfo()) {
      container.runInTransaction(em -> em.persist(new Drink("Chai Latte", "medium", "4.10")));
      System.out.println("name = " + container.getName() + ", count = " + count(container));
    }

    step("12. One shared factory, one EntityManager per thread");
    EntityManagerFactory shared = CoffeeShopDatabase.emf();
    try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
      for (String name : List.of("Latte", "Mocha", "Cortado", "Macchiato")) {
        pool.submit(() -> shared.runInTransaction(em -> em.persist(new Drink(name, "small", "3.60"))));
      }
    }
    System.out.println("count = " + count(shared)
        + ", same factory = " + (shared == CoffeeShopDatabase.emf()));

    step("13. Unknown persistence unit name");
    try {
      Persistence.createEntityManagerFactory("coffee");
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
    }
  }

  static long count(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from Drink", Long.class).getSingleResult());
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }
}
