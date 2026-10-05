package com.howtodoinjava.hibernate.saveorupdate.legacy;

import java.util.function.Consumer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/** Runs save(), update() and saveOrUpdate() on Hibernate 6.6 and prints the SQL at the call and at commit. */
@SuppressWarnings("deprecation")
public class LegacyDemo {

  private static SessionFactory sf;

  public static void main(String[] args) {
    sf = Database.create();
    try {
      Plant monstera = seed();

      step("1. save() a new plant", s -> {
        Plant basil = new Plant("Basil", "Ocimum basilicum", "small", 4.00);
        Object id = s.save(basil);
        System.out.println("   returned " + id + " (" + id.getClass().getSimpleName() + "), contains=" + s.contains(basil));
        printSql("at the call");
      });

      step("2. save() a managed plant", s -> {
        Plant managed = s.get(Plant.class, monstera.getId());
        SqlLog.clear();
        managed.setPrice(27.00);
        Object id = s.save(managed);
        System.out.println("   returned " + id);
        printSql("at the call");
      });

      Plant detached = sf.fromTransaction(s -> s.get(Plant.class, monstera.getId()));
      step("3. save() a detached plant", s -> {
        Object id = s.save(detached);
        System.out.println("   returned " + id + ", detached.getId()=" + detached.getId());
        printSql("at the call");
      });
      System.out.println("   rows: " + Database.count(sf));

      Plant d2 = sf.fromTransaction(s -> s.get(Plant.class, monstera.getId()));
      step("4. update() a detached plant, no change", s -> {
        s.update(d2);
        System.out.println("   contains(d2)=" + s.contains(d2));
        printSql("at the call");
      });

      Plant d3 = sf.fromTransaction(s -> s.get(Plant.class, monstera.getId()));
      d3.setPotSize("large");
      step("5. update() a detached plant, changed", s -> {
        s.update(d3);
        printSql("at the call");
      });

      tryStep("6. update() a new plant", s -> s.update(new Plant("Fern", "Nephrolepis exaltata", "medium", 9.00)));

      Plant d4 = sf.fromTransaction(s -> s.get(Plant.class, monstera.getId()));
      tryStep("7. update() when the id is already loaded", s -> {
        s.get(Plant.class, monstera.getId());
        s.update(d4);
      });

      Plant ghost = new Plant("Cactus", "Echinopsis", "small", 6.00);
      ghost.setId(99L);
      tryStep("8. update() an id that does not exist", s -> s.update(ghost));

      step("9. saveOrUpdate() a new plant", s -> {
        Plant fern = new Plant("Fern", "Nephrolepis exaltata", "medium", 9.00);
        s.saveOrUpdate(fern);
        System.out.println("   id=" + fern.getId() + ", contains=" + s.contains(fern));
        printSql("at the call");
      });

      Plant d5 = sf.fromTransaction(s -> s.get(Plant.class, monstera.getId()));
      d5.setPrice(30.00);
      step("10. saveOrUpdate() a detached plant", s -> {
        s.saveOrUpdate(d5);
        System.out.println("   contains=" + s.contains(d5));
        printSql("at the call");
      });

      Plant ghost2 = new Plant("Cactus", "Echinopsis", "small", 6.00);
      ghost2.setId(99L);
      tryStep("11. saveOrUpdate() an id that does not exist", s -> s.saveOrUpdate(ghost2));

      Plant d6 = sf.fromTransaction(s -> s.get(Plant.class, monstera.getId()));
      tryStep("12. persist() a detached plant (6.6)", s -> s.persist(d6));

      Plant ghost4 = new Plant("Cactus", "Echinopsis", "small", 6.00);
      ghost4.setId(99L);
      step("13. save() a plant with id 99", s -> {
        Object id = s.save(ghost4);
        System.out.println("   returned " + id + ", ghost4.getId()=" + ghost4.getId());
        printSql("at the call");
      });

      Plant ghost3 = new Plant("Cactus", "Echinopsis", "small", 6.00);
      ghost3.setId(99L);
      tryStep("14. merge() an id that does not exist (6.6)", s -> s.merge(ghost3));
      System.out.println("   rows: " + Database.count(sf));
    } finally {
      sf.close();
    }
  }

  private static Plant seed() {
    Plant monstera = new Plant("Monstera", "Monstera deliciosa", "medium", 25.00);
    sf.inTransaction(s -> s.persist(monstera));
    return monstera;
  }

  private static void step(String title, Consumer<Session> work) {
    System.out.println();
    System.out.println(title);
    SqlLog.clear();
    try (Session s = sf.openSession()) {
      Transaction tx = s.beginTransaction();
      work.accept(s);
      SqlLog.clear();
      tx.commit();
      printSql("at commit");
    }
  }

  private static void tryStep(String title, Consumer<Session> work) {
    try {
      step(title, work);
    } catch (RuntimeException e) {
      System.out.println("   " + e.getClass().getName() + ": " + e.getMessage());
      Throwable c = e.getCause();
      while (c != null) {
        System.out.println("   caused by " + c.getClass().getName() + ": " + c.getMessage());
        c = c.getCause();
      }
    }
  }

  private static void printSql(String when) {
    if (SqlLog.statements().isEmpty()) {
      System.out.println("   " + when + ": no SQL");
    }
    SqlLog.statements().forEach(sql -> System.out.println("   " + when + ": " + sql));
  }
}
