package com.howtodoinjava.hibernate.callbacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntityCallbacksTest {

  static EntityManagerFactory emf;
  static EntityManagerFactory riskyEmf;

  @BeforeAll
  static void start() {
    emf = Database.create(false);
    riskyEmf = Database.createRisky(false);
    CallbackLog.print(false);
  }

  @AfterAll
  static void stop() {
    emf.close();
    riskyEmf.close();
  }

  @BeforeEach
  void clearLog() {
    CallbackLog.clear();
  }

  @AfterEach
  void resetRisky() {
    RiskyListener.mode = RiskyListener.Mode.OFF;
    RiskyListener.em = null;
  }

  private Long save(String description, String amount) {
    return save(emf, description, amount);
  }

  private Long save(EntityManagerFactory emf, String description, String amount) {
    Long id = emf.callInTransaction(em -> {
      ExpenseClaim claim = new ExpenseClaim(description, new BigDecimal(amount));
      em.persist(claim);
      return claim.getId();
    });
    CallbackLog.clear();
    return id;
  }

  /** Callback events only, with SQL shortened to its first three words. */
  private static List<String> compact(List<String> events) {
    List<String> out = new ArrayList<>();
    for (String e : events) {
      if (e.startsWith("sql: ")) {
        String[] w = e.substring(5).split(" ");
        out.add("sql: " + w[0] + " " + w[1] + " " + w[2]);
      } else {
        out.add(e);
      }
    }
    return out;
  }

  @Test
  void identityIdRunsInsertAndPostPersistInsidePersistCall() {
    List<List<String>> snapshots = new ArrayList<>();
    ExpenseClaim lunch = emf.callInTransaction(em -> {
      ExpenseClaim claim = new ExpenseClaim("Team lunch", new BigDecimal("40.00"));
      em.persist(claim);
      snapshots.add(CallbackLog.events());
      return claim;
    });
    assertNotNull(lunch.getId());
    assertEquals(List.of(
        "AuditListener @PrePersist ExpenseClaim",
        "ClaimValidationListener validate",
        "ExpenseClaim @PrePersist id=null",
        "sql: insert into ExpenseClaim",
        "ClaimNotificationListener notify manager: claim " + lunch.getId() + " submitted",
        "ExpenseClaim @PostPersist id=" + lunch.getId()), compact(snapshots.get(0)));
  }

  @Test
  void sequenceIdIsReadyInPrePersistButInsertWaitsForFlush() {
    List<List<String>> snapshots = new ArrayList<>();
    emf.runInTransaction(em -> {
      TravelClaim taxi = new TravelClaim("Taxi", new BigDecimal("25.00"));
      em.persist(taxi);
      snapshots.add(CallbackLog.events());
      assertNotNull(taxi.getId());
      em.flush();
      snapshots.add(CallbackLog.events());
    });
    List<String> afterPersist = compact(snapshots.get(0));
    assertEquals("sql: select next value", afterPersist.get(0));
    assertTrue(afterPersist.get(1).startsWith("TravelClaim @PrePersist id="));
    assertFalse(afterPersist.get(1).contains("id=null"));   // the id is already set
    assertEquals(2, afterPersist.size());
    List<String> afterFlush = compact(snapshots.get(1));
    assertEquals("sql: insert into TravelClaim", afterFlush.get(2));
    assertTrue(afterFlush.get(3).startsWith("TravelClaim @PostPersist"));
  }

  @Test
  void excludeDefaultListenersSkipsAuditListener() {
    emf.runInTransaction(em -> em.persist(new TravelClaim("Bus", new BigDecimal("3.00"))));
    assertTrue(CallbackLog.events().stream().noneMatch(e -> e.startsWith("AuditListener")));
  }

  @Test
  void prePersistSetsSubmittedAtAndStatus() {
    ExpenseClaim lunch = emf.callInTransaction(em -> {
      ExpenseClaim claim = new ExpenseClaim("Team lunch", new BigDecimal("40.00"));
      assertNull(claim.getSubmittedAt());
      em.persist(claim);
      assertNotNull(claim.getSubmittedAt());
      return claim;
    });
    assertEquals(ClaimStatus.SUBMITTED, lunch.getStatus());
    assertNull(lunch.getUpdatedAt());
  }

  @Test
  void creationTimestampIsSetAtFlushNotAtPersist() {
    emf.runInTransaction(em -> {
      TravelClaim taxi = new TravelClaim("Taxi", new BigDecimal("25.00"));
      em.persist(taxi);
      assertNull(taxi.getSubmittedAt());
      em.flush();
      assertNotNull(taxi.getSubmittedAt());
      assertNotNull(taxi.getUpdatedAt());   // @UpdateTimestamp is set on insert too
    });
  }

  @Test
  void updateCallbacksRunAtCommitAroundTheUpdate() {
    Long id = save("Team lunch", "40.00");
    List<List<String>> snapshots = new ArrayList<>();
    emf.runInTransaction(em -> {
      ExpenseClaim lunch = em.find(ExpenseClaim.class, id);
      lunch.setStatus(ClaimStatus.APPROVED);
      snapshots.add(CallbackLog.events());
    });
    assertEquals(List.of("sql: select ec1_0.id,ec1_0.amount,ec1_0.description,ec1_0.status,ec1_0.submittedAt,ec1_0.updatedAt from",
        "ExpenseClaim @PostLoad"), compact(snapshots.get(0)));
    List<String> all = compact(CallbackLog.events());
    assertEquals(List.of(
        "AuditListener @PreUpdate ExpenseClaim",
        "ClaimValidationListener validate",
        "ExpenseClaim @PreUpdate",
        "sql: update ExpenseClaim set",
        "ClaimNotificationListener notify employee: claim " + id + " is APPROVED",
        "ExpenseClaim @PostUpdate"), all.subList(2, all.size()));
    ExpenseClaim reloaded = emf.callInTransaction(em -> em.find(ExpenseClaim.class, id));
    assertNotNull(reloaded.getUpdatedAt());   // the change made in @PreUpdate is saved
  }

  @Test
  void preUpdateIsNotCalledWhenNothingChanged() {
    Long id = save("Team lunch", "40.00");
    emf.runInTransaction(em -> em.find(ExpenseClaim.class, id).setAmount(new BigDecimal("40.00")));
    assertTrue(CallbackLog.events().stream().noneMatch(e -> e.contains("PreUpdate")));
  }

  @Test
  void bulkUpdateSkipsCallbacks() {
    Long id = save("Hotel", "90.00");
    int rows = emf.callInTransaction(em -> em.createQuery("update ExpenseClaim c set c.status = :s where c.id = :id")
        .setParameter("s", ClaimStatus.APPROVED).setParameter("id", id).executeUpdate());
    assertEquals(1, rows);
    assertEquals(List.of("sql: update ExpenseClaim ec1_0"), compact(CallbackLog.events()));
    ExpenseClaim reloaded = emf.callInTransaction(em -> em.find(ExpenseClaim.class, id));
    assertEquals(ClaimStatus.APPROVED, reloaded.getStatus());
    assertNull(reloaded.getUpdatedAt());   // @PreUpdate did not run
  }

  @Test
  void bulkDeleteSkipsPreRemove() {
    Long id = save("Snacks", "12.00");
    int rows = emf.callInTransaction(em -> em.createQuery("delete from ExpenseClaim c where c.id = :id")
        .setParameter("id", id).executeUpdate());
    assertEquals(1, rows);
    assertTrue(CallbackLog.events().stream().noneMatch(e -> e.contains("Remove")));
  }

  @Test
  void postLoadRunsOnFindRefreshAndQueryRows() {
    Long id = save("Team lunch", "40.00");
    save("Taxi", "25.00");
    emf.runInTransaction(em -> {
      ExpenseClaim lunch = em.find(ExpenseClaim.class, id);
      assertTrue(lunch.isLoaded());
      em.refresh(lunch);
      // the query returns the managed lunch again (no @PostLoad) and loads every other row (one each)
      int rows = em.createQuery("select c from ExpenseClaim c", ExpenseClaim.class).getResultList().size();
      long postLoads = CallbackLog.events().stream().filter(e -> e.equals("ExpenseClaim @PostLoad")).count();
      assertEquals(2 + (rows - 1), postLoads);
    });
  }

  @Test
  void removeCallbacks() {
    Long id = save("Team lunch", "40.00");
    List<List<String>> snapshots = new ArrayList<>();
    emf.runInTransaction(em -> {
      ExpenseClaim lunch = em.find(ExpenseClaim.class, id);
      CallbackLog.clear();
      em.remove(lunch);
      snapshots.add(CallbackLog.events());
    });
    assertEquals(List.of("AuditListener @PreRemove ExpenseClaim", "ExpenseClaim @PreRemove"), snapshots.get(0));
    assertEquals(List.of("AuditListener @PreRemove ExpenseClaim", "ExpenseClaim @PreRemove",
        "sql: delete from ExpenseClaim", "ExpenseClaim @PostRemove"), compact(CallbackLog.events()));
  }

  @Test
  void exceptionInListenerStopsPersistAndRollsBack() {
    long before = emf.callInTransaction(em -> em.createQuery("select count(c) from ExpenseClaim c", Long.class).getSingleResult());
    CallbackLog.clear();
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
        emf.runInTransaction(em -> em.persist(new ExpenseClaim("Coffee", new BigDecimal("-5.00")))));
    assertEquals("Claim amount must be positive: -5.00", e.getMessage());
    assertEquals(List.of("AuditListener @PrePersist ExpenseClaim", "ClaimValidationListener validate"), CallbackLog.events());
    long after = emf.callInTransaction(em -> em.createQuery("select count(c) from ExpenseClaim c", Long.class).getSingleResult());
    assertEquals(before, after);
  }

  @Test
  void commitListenerRunsAfterCommit() {
    emf.runInTransaction(em -> {
      em.persist(new ExpenseClaim("Team lunch", new BigDecimal("40.00")));
      CallbackLog.add("-- commit");
    });
    List<String> events = CallbackLog.events();
    assertEquals("-- commit", events.get(events.size() - 2));
    assertTrue(events.getLast().startsWith("ClaimCommitListener committed claim "));
  }

  @Test
  void commitListenerReportsRollback() {
    assertThrows(IllegalStateException.class, () -> emf.runInTransaction(em -> {
      em.persist(new ExpenseClaim("Team lunch", new BigDecimal("40.00")));
      throw new IllegalStateException("cancel");
    }));
    assertTrue(CallbackLog.events().stream().noneMatch(e -> e.startsWith("ClaimCommitListener committed")));
    assertTrue(CallbackLog.events().getLast().startsWith("ClaimCommitListener commit failed"), CallbackLog.events().toString());
  }

  @Test
  void queryInPreUpdateOverflowsTheStack() {
    Long id = save(riskyEmf, "Hotel", "90.00");
    RollbackException e = assertThrows(RollbackException.class, () -> riskyEmf.runInTransaction(em -> {
      RiskyListener.em = em;
      RiskyListener.mode = RiskyListener.Mode.QUERY_IN_PRE_UPDATE;
      em.find(ExpenseClaim.class, id).setAmount(new BigDecimal("95.00"));
    }));
    assertEquals("Error while committing the transaction [java.lang.StackOverflowError]", e.getMessage());
    RiskyListener.mode = RiskyListener.Mode.OFF;
    assertEquals(new BigDecimal("90.00"), riskyEmf.callInTransaction(em -> em.find(ExpenseClaim.class, id)).getAmount());
  }

  @Test
  void persistInPostPersistIsInsertedAtCommit() {
    long before = riskyEmf.callInTransaction(em -> em.createQuery("select count(h) from ClaimHistory h", Long.class).getSingleResult());
    CallbackLog.clear();
    riskyEmf.runInTransaction(em -> {
      RiskyListener.em = em;
      RiskyListener.mode = RiskyListener.Mode.PERSIST_IN_POST_PERSIST;
      em.persist(new ExpenseClaim("Train ticket", new BigDecimal("30.00")));
      CallbackLog.add("-- commit");
    });
    RiskyListener.mode = RiskyListener.Mode.OFF;
    List<String> events = compact(CallbackLog.events());
    int commit = events.indexOf("-- commit");
    assertEquals("sql: insert into ClaimHistory", events.get(commit + 1));
    long after = riskyEmf.callInTransaction(em -> em.createQuery("select count(h) from ClaimHistory h", Long.class).getSingleResult());
    assertEquals(before + 1, after);
  }

  @Test
  void changeInPostPersistCausesExtraUpdate() {
    Long id = riskyEmf.callInTransaction(em -> {
      RiskyListener.mode = RiskyListener.Mode.CHANGE_IN_POST_PERSIST;
      ExpenseClaim parking = new ExpenseClaim("Parking", new BigDecimal("8.00"));
      em.persist(parking);
      return parking.getId();
    });
    RiskyListener.mode = RiskyListener.Mode.OFF;
    List<String> sql = CallbackLog.events().stream().filter(e -> e.startsWith("sql: ")).map(e -> e.split(" ")[1]).toList();
    assertEquals(List.of("insert", "update"), sql);
    assertTrue(CallbackLog.events().contains("ExpenseClaim @PreUpdate"));
    assertEquals("Parking #" + id, riskyEmf.callInTransaction(em -> em.find(ExpenseClaim.class, id)).getDescription());
  }

  @Test
  void changeInPostUpdateIsLost() {
    Long id = save(riskyEmf, "Train ticket", "30.00");
    riskyEmf.runInTransaction(em -> {
      RiskyListener.mode = RiskyListener.Mode.CHANGE_IN_POST_UPDATE;
      ExpenseClaim train = em.find(ExpenseClaim.class, id);
      train.setAmount(new BigDecimal("35.00"));
    });
    RiskyListener.mode = RiskyListener.Mode.OFF;
    ExpenseClaim reloaded = riskyEmf.callInTransaction(em -> em.find(ExpenseClaim.class, id));
    assertEquals(new BigDecimal("35.00"), reloaded.getAmount());
    assertEquals("Train ticket", reloaded.getDescription());
  }

  @Test
  void mergeOfNewEntityRunsPrePersistOnTheManagedCopy() {
    ExpenseClaim snacks = new ExpenseClaim("Snacks", new BigDecimal("12.00"));
    ExpenseClaim managed = emf.callInTransaction(em -> em.merge(snacks));
    assertTrue(CallbackLog.events().contains("ExpenseClaim @PrePersist id=null"));
    assertNotNull(managed.getSubmittedAt());
    assertNull(snacks.getSubmittedAt());
  }
}
