package com.howtodoinjava.hibernate.namedprocedure;

import jakarta.persistence.EntityManagerFactory;
import org.testcontainers.mysql.MySQLContainer;

public class NamedProcedureDemo {

  public static void main(String[] args) {
    try (MySQLContainer mysql = Database.startMySql();
         EntityManagerFactory emf = Database.create(mysql, true)) {

      emf.runInTransaction(em -> {
        em.persist(new StockItem("bolt", "north", 120));
        em.persist(new StockItem("nut", "north", 40));
        em.persist(new StockItem("washer", "north", 15));
        em.persist(new StockItem("bolt", "south", 60));
        em.persist(new StockItem("screw", "south", 8));
      });

      System.out.println("\n== 1. OUT parameter ==");
      emf.runInTransaction(em ->
          System.out.println("units in north: " + StockProcedures.countUnits(em, "north")));

      System.out.println("\n== 2. Positional parameters ==");
      emf.runInTransaction(em ->
          System.out.println("units in south: " + StockProcedures.countUnitsByPosition(em, "south")));

      System.out.println("\n== 3. resultClasses = StockItem.class ==");
      emf.runInTransaction(em ->
          System.out.println(StockProcedures.findLowStock(em, "north", 50)));

      System.out.println("\n== 4. resultSetMappings to a record ==");
      emf.runInTransaction(em -> StockProcedures.summary(em).forEach(System.out::println));

      System.out.println("\n== 5. INOUT parameter ==");
      emf.runInTransaction(em ->
          System.out.println("screw in south after restock: " + StockProcedures.restock(em, "screw", "south", 50)));

      System.out.println("\n== 6. Timeout hint ==");
      emf.runInTransaction(em -> {
        System.out.println("hint: " + em.createNamedStoredProcedureQuery("StockItem.audit")
            .getHints().get("jakarta.persistence.query.timeout"));
        long start = System.currentTimeMillis();
        StockProcedures.audit(em, 10_000_000);
        System.out.println("finished after " + (System.currentTimeMillis() - start) + " ms");
      });

      System.out.println("\n== 7. Errors ==");
      run("wrong query name", () -> emf.runInTransaction(em ->
          em.createNamedStoredProcedureQuery("StockItem.countUnit")));
      run("wrong parameter name", () -> emf.runInTransaction(em ->
          em.createNamedStoredProcedureQuery("StockItem.countUnits").setParameter("warehouse", "north")));
      run("parameter not set", () -> emf.runInTransaction(em ->
          em.createNamedStoredProcedureQuery("StockItem.countUnits").execute()));
      run("createNamedQuery", () -> emf.runInTransaction(em ->
          em.createNamedQuery("StockItem.countUnits")));
      run("missing procedure", () -> emf.runInTransaction(em ->
          em.createNamedStoredProcedureQuery("StockItem.missingProcedure").execute()));
      run("record in resultClasses", () -> emf.runInTransaction(em ->
          em.createNamedStoredProcedureQuery("StockItem.summaryAsRecord").getResultList()));
      run("parameters in wrong order", () -> emf.runInTransaction(em ->
          em.createNamedStoredProcedureQuery("StockItem.countUnitsSwapped")
              .setParameter("p_warehouse", "north").execute()));
      run("JDBC timeout", () -> emf.runInTransaction(em ->
          StockProcedures.auditWithJdbcTimeout(em, 30_000_000, 1)));
    }
  }

  private static void run(String label, Runnable action) {
    try {
      action.run();
      System.out.println(label + ": no error");
    } catch (RuntimeException e) {
      System.out.println(label + ": " + e);
    }
  }
}
