package com.howtodoinjava.hibernate.namedprocedure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import java.sql.CallableStatement;
import java.util.List;
import org.hibernate.Session;

// Calls every @NamedStoredProcedureQuery declared on StockItem, by name
public final class StockProcedures {

  private StockProcedures() {
  }

  // OUT parameter
  public static Integer countUnits(EntityManager em, String warehouse) {
    StoredProcedureQuery query = em.createNamedStoredProcedureQuery("StockItem.countUnits")
        .setParameter("p_warehouse", warehouse);
    query.execute();
    return (Integer) query.getOutputParameterValue("p_total");
  }

  // resultClasses = StockItem.class
  @SuppressWarnings("unchecked")
  public static List<StockItem> findLowStock(EntityManager em, String warehouse, int below) {
    return em.createNamedStoredProcedureQuery("StockItem.findLowStock")
        .setParameter("p_warehouse", warehouse)
        .setParameter("p_below", below)
        .getResultList();
  }

  // resultSetMappings = "WarehouseSummaryMapping"
  @SuppressWarnings("unchecked")
  public static List<WarehouseSummary> summary(EntityManager em) {
    return em.createNamedStoredProcedureQuery("StockItem.summary").getResultList();
  }

  // INOUT parameter: sends the quantity to add, returns the new quantity
  public static Integer restock(EntityManager em, String name, String warehouse, int added) {
    StoredProcedureQuery query = em.createNamedStoredProcedureQuery("StockItem.restock")
        .setParameter("p_name", name)
        .setParameter("p_warehouse", warehouse)
        .setParameter("p_quantity", added);
    query.execute();
    return (Integer) query.getOutputParameterValue("p_quantity");
  }

  // Positional parameters, starting at 1
  public static Integer countUnitsByPosition(EntityManager em, String warehouse) {
    StoredProcedureQuery query = em.createNamedStoredProcedureQuery("StockItem.countUnitsByPosition")
        .setParameter(1, warehouse);
    query.execute();
    return (Integer) query.getOutputParameterValue(2);
  }

  // hints = jakarta.persistence.query.timeout
  public static void audit(EntityManager em, int rounds) {
    em.createNamedStoredProcedureQuery("StockItem.audit")
        .setParameter("p_rounds", rounds)
        .execute();
  }

  // Workaround for the ignored timeout: call the procedure through JDBC with setQueryTimeout()
  public static void auditWithJdbcTimeout(EntityManager em, int rounds, int seconds) {
    em.unwrap(Session.class).doWork(connection -> {
      try (CallableStatement call = connection.prepareCall("{call stock_audit(?)}")) {
        call.setInt(1, rounds);
        call.setQueryTimeout(seconds);
        call.execute();
      }
    });
  }
}
