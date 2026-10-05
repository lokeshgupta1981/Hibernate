package com.howtodoinjava.hibernate.c3p0;

import com.mchange.v2.c3p0.PooledDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class C3p0Demo {

  public static void main(String[] args) {

    step("1. Hibernate's built-in pool (not for production)");
    try (EntityManagerFactory emf = Database.builtIn(false)) {
      System.out.println("ConnectionProvider: " + Database.connectionProvider(emf).getClass().getSimpleName());
    }

    step("2. c3p0 configured with hibernate.c3p0.* settings");
    try (EntityManagerFactory emf = Database.c3p0(Database.C3P0_SETTINGS, true)) {
      PooledDataSource pool = Database.pool(emf);
      System.out.println("ConnectionProvider: " + Database.connectionProvider(emf).getClass().getSimpleName());
      Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5));   // check-ins are asynchronous
      System.out.println("After startup: " + Database.stats(pool));

      step("3. A transaction borrows one connection and returns it at commit");
      emf.runInTransaction(em -> {
        em.persist(new ParkingEntry("ab-123", LocalDateTime.of(2026, 10, 3, 8, 15)));
        em.persist(new ParkingEntry("cd-456", LocalDateTime.of(2026, 10, 3, 8, 40)));
        em.flush();
        System.out.println("Inside the transaction: " + Database.stats(pool));
      });
      Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5));
      System.out.println("After commit: " + Database.stats(pool));
      System.out.println("Entries saved: " + Database.countEntries(emf));

      step("4. Seven transactions at once: the pool grows by acquire_increment (5)");
      List<EntityManager> held = Database.holdConnections(emf, 7);
      Database.await(() -> Database.total(pool) == 10, Duration.ofSeconds(5));
      System.out.println("7 open transactions: " + Database.stats(pool));
      Database.release(held);
      Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5));
      System.out.println("After they finish: " + Database.stats(pool));
    }

    step("5. Pool exhausted: max_size 2, checkoutTimeout 1000 ms");
    Map<String, Object> small = Map.of(
        "hibernate.c3p0.min_size", 1,
        "hibernate.c3p0.max_size", 2,
        "hibernate.c3p0.checkoutTimeout", 1000);
    try (EntityManagerFactory emf = Database.c3p0(small, false)) {
      List<EntityManager> held = Database.holdConnections(emf, 2);
      long start = System.nanoTime();
      try {
        Database.holdConnections(emf, 1);
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
        Throwable root = e;
        while (root.getCause() != null) {
          root = root.getCause();
        }
        System.out.println("Root cause: " + root.getClass().getName() + ": " + root.getMessage());
        System.out.println("Waited about " + Duration.ofNanos(System.nanoTime() - start).toMillis() / 100 * 100 + " ms");
      }
      Database.release(held);
    }

    step("6. Extra idle connections are closed after maxIdleTimeExcessConnections (2 s)");
    Map<String, Object> shrinking = Map.of(
        "hibernate.c3p0.min_size", 2,
        "hibernate.c3p0.max_size", 10,
        "hibernate.c3p0.acquire_increment", 2,
        "hibernate.c3p0.maxIdleTimeExcessConnections", 2);
    try (EntityManagerFactory emf = Database.c3p0(shrinking, false)) {
      PooledDataSource pool = Database.pool(emf);
      Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5));
      List<EntityManager> held = Database.holdConnections(emf, 6);
      System.out.println("6 open transactions: " + Database.stats(pool));
      Database.release(held);
      Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5));
      System.out.println("Right after release: " + Database.stats(pool));
      Database.await(() -> Database.total(pool) == 2, Duration.ofSeconds(10));
      System.out.println("A few seconds later: " + Database.stats(pool));
    }

    step("7. Leak detection: unreturnedConnectionTimeout 2 s");
    Map<String, Object> leak = Map.of(
        "hibernate.c3p0.unreturnedConnectionTimeout", 2,
        "hibernate.c3p0.debugUnreturnedConnectionStackTraces", true);
    try (EntityManagerFactory emf = Database.c3p0(leak, false)) {
      PooledDataSource pool = Database.pool(emf);
      Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5));
      EntityManager forgotten = emf.createEntityManager();
      forgotten.getTransaction().begin();       // never committed, never closed
      System.out.println("Leaked transaction open: " + Database.stats(pool));
      Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(10));
      System.out.println("After the timeout: " + Database.stats(pool));
    }

    step("8. Database restart: broken pooled connections");
    restart(false);
    restart(true);
  }

  /** Shuts down a file-based H2 database to simulate a database restart. */
  static void restart(boolean testOnCheckout) {
    long count = restartAndCount(testOnCheckout);
    System.out.println("testConnectionOnCheckout=" + testOnCheckout + ", entries after restart: "
        + (count < 0 ? "query failed" : count));
  }

  /** Returns the number of entries read after the restart, or -1 when the query fails. */
  static long restartAndCount(boolean testOnCheckout) {
    Map<String, Object> settings = new HashMap<>();
    settings.put("hibernate.c3p0.min_size", 3);
    settings.put("hibernate.c3p0.testConnectionOnCheckout", testOnCheckout);
    // create the table at startup, but do not drop it at close (the database is down by then)
    settings.put("jakarta.persistence.schema-generation.database.action", "drop-and-create");
    String url = "jdbc:h2:./target/garage-restart-" + testOnCheckout;
    try (EntityManagerFactory emf = Database.create(url, settings, false)) {
      emf.runInTransaction(em -> em.persist(new ParkingEntry("ab-123", LocalDateTime.of(2026, 10, 3, 8, 15))));
      Database.shutdown(emf);
      try {
        return Database.countEntries(emf);
      } catch (RuntimeException e) {
        Throwable root = e;
        while (root.getCause() != null) {
          root = root.getCause();
        }
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
        System.out.println("Root cause: " + root.getClass().getName() + ": "
            + root.getMessage().lines().findFirst().orElse(""));
        return -1;
      }
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title);
  }
}
