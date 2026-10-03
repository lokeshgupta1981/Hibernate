package com.howtodoinjava.hibernate.namedprocedure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.StoredProcedureQuery;
import java.util.List;
import org.hibernate.JDBCException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.testcontainers.mysql.MySQLContainer;

class NamedProcedureTest {

  static MySQLContainer mysql;
  static EntityManagerFactory emf;

  @BeforeAll
  static void startDatabase() {
    mysql = Database.startMySql();
    emf = Database.create(mysql, false);
  }

  @AfterAll
  static void stopDatabase() {
    emf.close();
    mysql.stop();
  }

  @BeforeEach
  void insertStock() {
    emf.runInTransaction(em -> {
      em.createQuery("delete from StockItem").executeUpdate();
      em.persist(new StockItem("bolt", "north", 120));
      em.persist(new StockItem("nut", "north", 40));
      em.persist(new StockItem("washer", "north", 15));
      em.persist(new StockItem("bolt", "south", 60));
      em.persist(new StockItem("screw", "south", 8));
    });
  }

  @Test
  void readsOutParameter() {
    assertEquals(Integer.valueOf(175), emf.<Integer>callInTransaction(em -> StockProcedures.countUnits(em, "north")));
  }

  @Test
  void mapsRowsToEntitiesWithResultClasses() {
    List<String> items = emf.callInTransaction(em ->
        StockProcedures.findLowStock(em, "north", 50).stream().map(StockItem::toString).toList());
    assertEquals(List.of("washer (north, 15)", "nut (north, 40)"), items);
  }

  @Test
  void executeReturnsWhetherFirstResultIsResultSet() {
    List<Boolean> results = emf.callInTransaction(em -> List.of(
        em.createNamedStoredProcedureQuery("StockItem.findLowStock")
            .setParameter("p_warehouse", "north").setParameter("p_below", 50).execute(),
        em.createNamedStoredProcedureQuery("StockItem.countUnits")
            .setParameter("p_warehouse", "north").execute()));
    assertEquals(List.of(true, false), results);
  }

  @Test
  void returnedEntitiesAreManaged() {
    assertTrue(emf.<Boolean>callInTransaction(em -> em.contains(StockProcedures.findLowStock(em, "north", 50).get(0))));
  }

  @Test
  void changesToReturnedEntitiesAreSavedAtCommit() {
    emf.runInTransaction(em -> StockProcedures.findLowStock(em, "north", 50).get(0).setQuantity(25));
    assertEquals(Integer.valueOf(185), emf.<Integer>callInTransaction(em -> StockProcedures.countUnits(em, "north")));
  }

  @Test
  void unknownOutputParameterName() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> emf.runInTransaction(em -> {
      StoredProcedureQuery query = em.createNamedStoredProcedureQuery("StockItem.countUnits")
          .setParameter("p_warehouse", "north");
      query.execute();
      query.getOutputParameterValue("total");
    }));
    assertEquals("Named parameter [total] is not registered with this procedure call", e.getMessage());
  }

  @Test
  void mapsRowsToRecordWithResultSetMapping() {
    assertEquals(List.of(new WarehouseSummary("north", 3L, 175L), new WarehouseSummary("south", 2L, 68L)),
        emf.callInTransaction(StockProcedures::summary));
  }

  @Test
  void inoutParameterSendsAndReturnsValue() {
    assertEquals(Integer.valueOf(58), emf.<Integer>callInTransaction(em -> StockProcedures.restock(em, "screw", "south", 50)));
    assertEquals(Integer.valueOf(118), emf.<Integer>callInTransaction(em -> StockProcedures.countUnits(em, "south")));
  }

  @Test
  void positionalParameters() {
    assertEquals(Integer.valueOf(68), emf.<Integer>callInTransaction(em -> StockProcedures.countUnitsByPosition(em, "south")));
  }

  @Test
  void timeoutHintIsStoredButNotApplied() {
    Object hint = emf.callInTransaction(em -> em.createNamedStoredProcedureQuery("StockItem.audit")
        .getHints().get("jakarta.persistence.query.timeout"));
    assertEquals("1000", String.valueOf(hint));
    long start = System.currentTimeMillis();
    emf.runInTransaction(em -> StockProcedures.audit(em, 10_000_000));
    assertTrue(System.currentTimeMillis() - start > 1000);
  }

  @Test
  void jdbcQueryTimeoutCancelsTheCall() {
    JDBCException e = assertThrows(JDBCException.class, () -> emf.runInTransaction(em ->
        StockProcedures.auditWithJdbcTimeout(em, 30_000_000, 1)));
    assertTrue(e.getSQLException().getMessage().contains("Statement cancelled due to timeout"));
  }

  @Test
  void unknownQueryName() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> emf.runInTransaction(em ->
        em.createNamedStoredProcedureQuery("StockItem.countUnit")));
    assertEquals("No @NamedStoredProcedureQuery was found with that name : StockItem.countUnit", e.getMessage());
  }

  @Test
  void createNamedQueryDoesNotFindProcedures() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> emf.runInTransaction(em ->
        em.createNamedQuery("StockItem.countUnits")));
    assertEquals("No query named 'StockItem.countUnits'", e.getMessage());
  }

  @Test
  void unknownParameterName() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> emf.runInTransaction(em ->
        em.createNamedStoredProcedureQuery("StockItem.countUnits").setParameter("warehouse", "north")));
    assertEquals("Named parameter [warehouse] is not registered with this procedure call", e.getMessage());
  }

  @Test
  void parameterNotSet() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> emf.runInTransaction(em ->
        em.createNamedStoredProcedureQuery("StockItem.countUnits").execute()));
    assertEquals("The parameter named 'p_warehouse' was not set", e.getMessage());
  }

  @Test
  void parameterNamesDifferentFromProcedureStillWork() {
    Integer total = emf.callInTransaction(em -> {
      StoredProcedureQuery query = em.createNamedStoredProcedureQuery("StockItem.countUnitsRenamed")
          .setParameter("warehouse", "north");
      query.execute();
      return (Integer) query.getOutputParameterValue("total");
    });
    assertEquals(175, total);
  }

  @Test
  void parametersInWrongOrder() {
    GenericJDBCException e = assertThrows(GenericJDBCException.class, () -> emf.runInTransaction(em ->
        em.createNamedStoredProcedureQuery("StockItem.countUnitsSwapped").setParameter("p_warehouse", "north").execute()));
    assertTrue(e.getMessage().startsWith("Unable to register CallableStatement OUT parameter [Parameter number 1 is not an OUT parameter]"));
  }

  @Test
  void wrongProcedureNameFailsOnlyAtCallTime() {
    SQLGrammarException e = assertThrows(SQLGrammarException.class, () -> emf.runInTransaction(em ->
        em.createNamedStoredProcedureQuery("StockItem.missingProcedure").execute()));
    assertEquals("PROCEDURE test.count_unit does not exist", e.getSQLException().getMessage());
  }

  @Test
  void recordInResultClassesFails() {
    RuntimeException e = assertThrows(RuntimeException.class, () -> emf.runInTransaction(em ->
        em.createNamedStoredProcedureQuery("StockItem.summaryAsRecord").getResultList()));
    assertTrue(e.getMessage().startsWith("Could not determine recommended JdbcType for Java type"));
  }

  @Test
  void springDataCallsProcedureByName() {
    Integer total = emf.callInTransaction(em ->
        new JpaRepositoryFactory(em).getRepository(StockItemRepository.class).countUnits("north"));
    assertEquals(175, total);
  }
}
