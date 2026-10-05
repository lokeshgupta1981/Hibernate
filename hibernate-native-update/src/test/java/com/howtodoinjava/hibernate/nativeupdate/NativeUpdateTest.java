package com.howtodoinjava.hibernate.nativeupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TransactionRequiredException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.query.NativeQuery;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NativeUpdateTest {

  private EntityManagerFactory emf;
  private Map<String, Long> ids;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    ids = Database.seed(emf);
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private BigDecimal price(String name) {
    return Database.priceInTable(emf, name);
  }

  private static BigDecimal money(String value) {
    return new BigDecimal(value);
  }

  private int raiseActivePrices(EntityManager em) {
    return em.createNativeQuery("update plan set monthly_price = monthly_price + :amount where active = true")
        .setParameter("amount", money("1.00"))
        .executeUpdate();
  }

  @Test
  void nativeUpdateReturnsTheNumberOfChangedRows() {
    int rows = emf.callInTransaction(this::raiseActivePrices);

    assertEquals(3, rows);
    assertEquals(money("8.99"), price("Basic"));
    assertEquals(money("13.99"), price("Standard"));
    assertEquals(money("19.99"), price("Premium"));
    assertEquals(money("9.99"), price("Legacy HD"));   // inactive, not changed
  }

  @Test
  void positionalParameters() {
    int rows = emf.callInTransaction(em -> em
        .createNativeQuery("update plan set name = ?1 where name = ?2")
        .setParameter(1, "Legacy")
        .setParameter(2, "Legacy HD")
        .executeUpdate());

    assertEquals(1, rows);
    assertEquals(money("9.99"), price("Legacy"));
  }

  @Test
  void namedNativeUpdateQuery() {
    emf.runInTransaction(this::raiseActivePrices);

    int rows = emf.callInTransaction(em -> em
        .createNamedQuery("Plan.deactivateCheaperThan")
        .setParameter("minPrice", money("10.00"))
        .executeUpdate());

    assertEquals(1, rows);   // Basic 8.99; Legacy HD is already inactive
    List<String> active = emf.callInTransaction(em -> em
        .createQuery("select p.name from Plan p where p.active = true order by p.id", String.class)
        .getResultList());
    assertEquals(List.of("Standard", "Premium"), active);
  }

  @Test
  void noMatchingRowReturnsZero() {
    int rows = emf.callInTransaction(em -> em
        .createNativeQuery("update plan set active = true where name = :name")
        .setParameter("name", "Ultra")
        .executeUpdate());

    assertEquals(0, rows);
  }

  @Test
  void clearDropsChangesThatWereNotFlushed() {
    emf.runInTransaction(em -> {
      em.find(Plan.class, ids.get("Basic")).setName("Basic Plus");
      em.clear();
    });

    List<String> names = emf.callInTransaction(em -> em
        .createQuery("select p.name from Plan p order by p.id", String.class)
        .getResultList());
    assertEquals("Basic", names.get(0));
  }

  @Test
  void executeUpdateNeedsATransaction() {
    try (EntityManager em = emf.createEntityManager()) {
      TransactionRequiredException e = assertThrows(TransactionRequiredException.class,
          () -> em.createNativeQuery("update plan set active = true").executeUpdate());
      assertEquals("No active transaction for update or delete query", e.getMessage());
    }
  }

  @Test
  void managedEntityKeepsTheOldValueUntilRefresh() {
    emf.runInTransaction(em -> {
      Plan premium = em.find(Plan.class, ids.get("Premium"));
      em.createNativeQuery("update plan set monthly_price = :price where name = :name")
          .setParameter("price", money("21.99"))
          .setParameter("name", "Premium")
          .executeUpdate();

      assertEquals(money("18.99"), premium.getMonthlyPrice());
      assertSame(premium, em.find(Plan.class, ids.get("Premium")));   // find() returns the cached object
      assertEquals(money("18.99"), em.find(Plan.class, ids.get("Premium")).getMonthlyPrice());

      em.refresh(premium);
      assertEquals(money("21.99"), premium.getMonthlyPrice());
    });
  }

  @Test
  void clearDetachesTheStaleEntity() {
    emf.runInTransaction(em -> {
      Plan premium = em.find(Plan.class, ids.get("Premium"));
      em.createNativeQuery("update plan set monthly_price = 21.99 where name = 'Premium'").executeUpdate();
      em.clear();

      Plan fresh = em.find(Plan.class, ids.get("Premium"));
      assertEquals(money("21.99"), fresh.getMonthlyPrice());
      assertEquals(money("18.99"), premium.getMonthlyPrice());   // old object, now detached
      assertFalse(em.contains(premium));
    });
  }

  @Test
  void staleEntityOverwritesTheNativeUpdateAtCommit() {
    emf.runInTransaction(em -> {
      Plan premium = em.find(Plan.class, ids.get("Premium"));
      em.createNativeQuery("update plan set monthly_price = 21.99 where name = 'Premium'").executeUpdate();
      premium.setName("Premium 4K");
    });

    assertEquals(money("18.99"), price("Premium 4K"));   // 21.99 is lost
  }

  @Test
  void refreshBeforeChangingKeepsTheNativeUpdate() {
    emf.runInTransaction(em -> {
      Plan premium = em.find(Plan.class, ids.get("Premium"));
      em.createNativeQuery("update plan set monthly_price = 21.99 where name = 'Premium'").executeUpdate();
      em.refresh(premium);
      premium.setName("Premium 4K");
    });

    assertEquals(money("21.99"), price("Premium 4K"));
  }

  @Test
  void pendingChangesAreFlushedBeforeTheNativeUpdate() {
    int rows = emf.callInTransaction(em -> {
      em.find(Plan.class, ids.get("Standard")).setActive(false);
      return raiseActivePrices(em);
    });

    assertEquals(2, rows);                          // Standard was flushed as inactive first
    assertEquals(money("12.99"), price("Standard"));
  }

  @Test
  void synchronizedWithPlanStillFlushes() {
    int rows = emf.callInTransaction(em -> {
      em.find(Plan.class, ids.get("Standard")).setActive(false);
      return em.createNativeQuery("update plan set monthly_price = monthly_price + 1 where active = true")
          .unwrap(NativeQuery.class)
          .addSynchronizedEntityClass(Plan.class)
          .executeUpdate();
    });

    assertEquals(2, rows);
  }

  @Test
  void synchronizedWithAnotherTableSkipsTheFlush() {
    int rows = emf.callInTransaction(em -> {
      em.find(Plan.class, ids.get("Standard")).setActive(false);
      return em.createNativeQuery("update plan set monthly_price = monthly_price + 1 where active = true")
          .unwrap(NativeQuery.class)
          .addSynchronizedQuerySpace("addon")
          .executeUpdate();
    });

    assertEquals(3, rows);                          // Standard was still active in the table
    assertEquals(money("12.99"), price("Standard")); // the flush at commit wrote back the old price
  }

  private void loadIntoSecondLevelCache() {
    emf.runInTransaction(em -> {
      em.find(Plan.class, ids.get("Basic"));
      em.find(Addon.class, ids.get("Sports"));
    });
    assertTrue(emf.getCache().contains(Plan.class, ids.get("Basic")));
    assertTrue(emf.getCache().contains(Addon.class, ids.get("Sports")));
  }

  @Test
  void unsynchronizedNativeUpdateClearsEveryCacheRegion() {
    loadIntoSecondLevelCache();

    emf.runInTransaction(this::raiseActivePrices);

    assertFalse(emf.getCache().contains(Plan.class, ids.get("Basic")));
    assertFalse(emf.getCache().contains(Addon.class, ids.get("Sports")));
  }

  @Test
  void synchronizedNativeUpdateClearsOnlyThePlanRegion() {
    loadIntoSecondLevelCache();

    emf.runInTransaction(em -> em
        .createNativeQuery("update plan set monthly_price = monthly_price + :amount where active = true")
        .setParameter("amount", money("1.00"))
        .unwrap(NativeQuery.class)
        .addSynchronizedEntityClass(Plan.class)
        .executeUpdate());

    assertFalse(emf.getCache().contains(Plan.class, ids.get("Basic")));
    assertTrue(emf.getCache().contains(Addon.class, ids.get("Sports")));
  }

  @Test
  void querySpaceNameWorksLikeTheEntityClass() {
    loadIntoSecondLevelCache();

    emf.runInTransaction(em -> em
        .createNativeQuery("update plan set active = true where name = 'Legacy HD'")
        .unwrap(NativeQuery.class)
        .addSynchronizedQuerySpace("plan")
        .executeUpdate());

    assertFalse(emf.getCache().contains(Plan.class, ids.get("Basic")));
    assertTrue(emf.getCache().contains(Addon.class, ids.get("Sports")));
  }

  @Test
  void namedNativeQueryWithSpacesHintClearsOnlyThePlanRegion() {
    loadIntoSecondLevelCache();

    int rows = emf.callInTransaction(em -> em.createNamedQuery("Plan.setPrice")
        .setParameter("price", money("9.49"))
        .setParameter("name", "Basic")
        .executeUpdate());

    assertEquals(1, rows);
    assertFalse(emf.getCache().contains(Plan.class, ids.get("Basic")));
    assertTrue(emf.getCache().contains(Addon.class, ids.get("Sports")));
  }

  @Test
  void jpqlBulkUpdateClearsOnlyThePlanRegion() {
    loadIntoSecondLevelCache();

    int rows = emf.callInTransaction(em -> em
        .createQuery("update Plan p set p.monthlyPrice = p.monthlyPrice + :amount where p.active = true")
        .setParameter("amount", money("1.00"))
        .executeUpdate());

    assertEquals(3, rows);
    assertFalse(emf.getCache().contains(Plan.class, ids.get("Basic")));
    assertTrue(emf.getCache().contains(Addon.class, ids.get("Sports")));
  }

  @Test
  void wrongQuerySpaceClearsTheWrongRegion() {
    loadIntoSecondLevelCache();

    emf.runInTransaction(em -> em
        .createNativeQuery("update plan set monthly_price = 8.49 where name = 'Basic'")
        .unwrap(NativeQuery.class)
        .addSynchronizedQuerySpace("addon")
        .executeUpdate());

    assertTrue(emf.getCache().contains(Plan.class, ids.get("Basic")));    // stale Basic stays cached
    assertFalse(emf.getCache().contains(Addon.class, ids.get("Sports")));
    BigDecimal cached = emf.callInTransaction(em -> em.find(Plan.class, ids.get("Basic")).getMonthlyPrice());
    assertEquals(money("7.99"), cached);                                  // from the cache
    assertEquals(money("8.49"), price("Basic"));                          // in the table
  }

  @Test
  void dirtyCheckingKeepsTheCacheEntryCurrent() {
    loadIntoSecondLevelCache();

    emf.runInTransaction(em -> em.find(Plan.class, ids.get("Basic")).setMonthlyPrice(money("8.49")));

    assertTrue(emf.getCache().contains(Plan.class, ids.get("Basic")));
    Statistics stats = Database.statistics(emf);
    stats.clear();
    BigDecimal cached = emf.callInTransaction(em -> em.find(Plan.class, ids.get("Basic")).getMonthlyPrice());
    assertEquals(money("8.49"), cached);
    assertEquals(1, stats.getSecondLevelCacheHitCount());
  }

  @Test
  void jpqlBulkUpdateAlsoLeavesLoadedEntitiesStale() {
    emf.runInTransaction(em -> {
      Plan basic = em.find(Plan.class, ids.get("Basic"));
      em.createQuery("update Plan p set p.monthlyPrice = p.monthlyPrice + :amount where p.active = true")
          .setParameter("amount", money("1.00"))
          .executeUpdate();
      assertEquals(money("7.99"), basic.getMonthlyPrice());
    });
    assertEquals(money("8.99"), price("Basic"));
  }

  private void readAddonsWithQueryCache() {
    emf.runInTransaction(em -> em.createQuery("select a from Addon a", Addon.class)
        .setHint("org.hibernate.cacheable", true)
        .getResultList());
  }

  @Test
  void unsynchronizedNativeUpdateInvalidatesUnrelatedQueryCacheResults() {
    Statistics stats = Database.statistics(emf);
    readAddonsWithQueryCache();
    readAddonsWithQueryCache();
    assertEquals(1, stats.getQueryCacheHitCount());

    emf.runInTransaction(this::raiseActivePrices);
    stats.clear();
    readAddonsWithQueryCache();

    assertEquals(0, stats.getQueryCacheHitCount());
    assertEquals(1, stats.getQueryCacheMissCount());
  }

  @Test
  void synchronizedNativeUpdateKeepsUnrelatedQueryCacheResults() {
    Statistics stats = Database.statistics(emf);
    readAddonsWithQueryCache();

    emf.runInTransaction(em -> em
        .createNativeQuery("update plan set monthly_price = monthly_price + 1 where active = true")
        .unwrap(NativeQuery.class)
        .addSynchronizedEntityClass(Plan.class)
        .executeUpdate());
    stats.clear();
    readAddonsWithQueryCache();

    assertEquals(1, stats.getQueryCacheHitCount());
    assertEquals(0, stats.getQueryCacheMissCount());
  }

  @Test
  void wrongQuerySpaceInvalidatesTheAddonQueryResults() {
    Statistics stats = Database.statistics(emf);
    readAddonsWithQueryCache();

    emf.runInTransaction(em -> em
        .createNativeQuery("update plan set active = true where name = 'Legacy HD'")
        .unwrap(NativeQuery.class)
        .addSynchronizedQuerySpace("addon")
        .executeUpdate());
    stats.clear();
    readAddonsWithQueryCache();

    assertEquals(0, stats.getQueryCacheHitCount());
    assertEquals(1, stats.getQueryCacheMissCount());
  }

  @Test
  void onlyDirtyCheckingRunsPreUpdateCallbacks() {
    Plan.preUpdateCalls = 0;
    emf.runInTransaction(em -> em.createQuery("select p from Plan p where p.active = true", Plan.class)
        .getResultList()
        .forEach(p -> p.setMonthlyPrice(p.getMonthlyPrice().add(money("1.00")))));
    assertEquals(3, Plan.preUpdateCalls);

    Plan.preUpdateCalls = 0;
    emf.runInTransaction(em -> em
        .createQuery("update Plan p set p.monthlyPrice = p.monthlyPrice + :amount where p.active = true")
        .setParameter("amount", money("1.00"))
        .executeUpdate());
    emf.runInTransaction(this::raiseActivePrices);
    assertEquals(0, Plan.preUpdateCalls);

    assertEquals(money("10.99"), price("Basic"));     // 7.99 + 3 x 1.00
  }

  @Test
  void getResultListOnAnUpdateFails() {
    GenericJDBCException e = assertThrows(GenericJDBCException.class, () -> emf.runInTransaction(em -> em
        .createNativeQuery("update plan set active = true").getResultList()));
    assertTrue(e.getMessage().contains("Method is only allowed for a query. Use execute or executeUpdate instead of executeQuery"));
  }

  @Test
  void executeUpdateOnASelectFails() {
    GenericJDBCException e = assertThrows(GenericJDBCException.class, () -> emf.runInTransaction(em -> em
        .createNativeQuery("select * from plan").executeUpdate()));
    assertTrue(e.getMessage().contains("Method is not allowed for a query. Use execute or executeQuery instead of executeUpdate"));
  }
}
