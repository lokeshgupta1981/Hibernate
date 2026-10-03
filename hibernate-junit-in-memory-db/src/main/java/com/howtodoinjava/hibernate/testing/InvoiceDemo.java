package com.howtodoinjava.hibernate.testing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;

/**
 * Runs the same steps as one JUnit test class and prints the SQL. Run with
 * "mvn -q test-compile exec:java" so that H2 and import.sql (test scope) are on the classpath.
 */
public class InvoiceDemo {

  public static void main(String[] args) {
    step("1. @BeforeAll: create the EntityManagerFactory (schema + import.sql)");
    try (EntityManagerFactory emf = Database.inMemory(true)) {

      step("2. @BeforeEach: truncate the tables and reload import.sql");
      emf.getSchemaManager().truncate();

      step("3. @Test: call the repository inside a transaction");
      EntityManager em = emf.createEntityManager();
      em.getTransaction().begin();
      InvoiceRepository repository = new InvoiceRepository(em);
      System.out.println("unpaid = " + repository.findUnpaid());
      Invoice maria = repository.save(new Invoice("Maria", "90.00", LocalDate.of(2026, 9, 25)));
      System.out.println("saved id = " + maria.getId());
      System.out.println("outstanding Lokesh = " + repository.outstandingFor("Lokesh"));

      step("4. @AfterEach: roll back and close the EntityManager");
      em.getTransaction().rollback();
      em.close();

      step("5. Timing: new factory per test vs truncate per test (10 runs each)");
    }
    timing();
    step("6. @AfterAll: the factory was closed (drop table)");
  }

  private static void timing() {
    long start = System.nanoTime();
    for (int i = 0; i < 10; i++) {
      try (EntityManagerFactory emf = Database.inMemory(false)) {
        emf.callInTransaction(em -> em.find(Invoice.class, 1L));
      }
    }
    long perFactory = (System.nanoTime() - start) / 10 / 1_000_000;

    try (EntityManagerFactory emf = Database.inMemory(false)) {
      start = System.nanoTime();
      for (int i = 0; i < 10; i++) {
        emf.getSchemaManager().truncate();
        emf.callInTransaction(em -> em.find(Invoice.class, 1L));
      }
      long perTruncate = (System.nanoTime() - start) / 10 / 1_000_000;
      System.out.println("new factory per test: about " + perFactory + " ms");
      System.out.println("truncate per test:    about " + perTruncate + " ms");
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title);
  }
}
