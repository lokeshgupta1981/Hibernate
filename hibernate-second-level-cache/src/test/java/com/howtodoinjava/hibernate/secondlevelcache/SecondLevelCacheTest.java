package com.howtodoinjava.hibernate.secondlevelcache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecondLevelCacheTest {

  private EntityManagerFactory emf;
  private Statistics stats;
  private Long indiaId;

  @BeforeEach
  void setUp() {
    emf = Database.create(false, Language.class, Continent.class, Ocean.class, Timezone.class, Landmark.class);
    stats = Database.statistics(emf);
    indiaId = SecondLevelCacheDemo.saveCountries(emf);
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private void startEmpty() {
    emf.getCache().evictAll();
    stats.clear();
  }

  private String findCapital() {
    return emf.callInTransaction(em -> em.find(Country.class, indiaId).getCapital());
  }

  @Test
  void persistPutsNewEntitiesIntoTheCache() {
    assertEquals(7, stats.getSecondLevelCachePutCount());     // 4 countries + 3 currencies
    assertTrue(emf.getCache().contains(Country.class, indiaId));
    assertTrue(emf.getCache().contains(Currency.class, 1L));
  }

  @Test
  void secondSessionReadsFromTheCacheWithoutSql() {
    startEmpty();
    findCapital();
    assertEquals(1, stats.getSecondLevelCacheMissCount());
    assertEquals(1, stats.getSecondLevelCachePutCount());
    assertEquals(1, stats.getPrepareStatementCount());

    assertEquals("New Delhi", findCapital());
    assertEquals(1, stats.getSecondLevelCacheHitCount());
    assertEquals(1, stats.getPrepareStatementCount());       // no new SELECT
  }

  @Test
  void sameSessionUsesTheFirstLevelCache() {
    startEmpty();
    emf.runInTransaction(em -> {
      Country first = em.find(Country.class, indiaId);
      Country second = em.find(Country.class, indiaId);
      assertSame(first, second);
    });
    assertEquals(0, stats.getSecondLevelCacheHitCount());
    assertEquals(1, stats.getSecondLevelCacheMissCount());
    assertEquals(1, stats.getPrepareStatementCount());
  }

  @Test
  void readWriteEntryPutBySessionIsNotReadableInThatSession() {
    startEmpty();
    emf.runInTransaction(em -> {
      Country first = em.find(Country.class, indiaId);
      em.clear();
      Country again = em.find(Country.class, indiaId);
      assertNotSame(first, again);
    });
    assertEquals(0, stats.getSecondLevelCacheHitCount());
    assertEquals(2, stats.getSecondLevelCacheMissCount());
    assertEquals(2, stats.getPrepareStatementCount());

    // a session opened later can read it
    emf.runInTransaction(em -> {
      em.find(Country.class, indiaId);
      em.clear();
      em.find(Country.class, indiaId);
    });
    assertEquals(2, stats.getSecondLevelCacheHitCount());
    assertEquals(2, stats.getPrepareStatementCount());
  }

  @Test
  void entityEntryHoldsDehydratedState() {
    startEmpty();
    emf.runInTransaction(em -> em.find(Country.class, indiaId).getCurrencies().size());
    String country = Country.class.getName();
    assertEquals(country + "#1 -> CacheEntry [New Delhi, 1, India]",
        SecondLevelCacheDemo.dump(emf, country, country + "#1"));
    assertEquals(country + ".currencies#1 -> CollectionCacheEntry [1]",
        SecondLevelCacheDemo.dump(emf, country + ".currencies", country + ".currencies#1"));
    assertEquals(Currency.class.getName() + "#1 -> CacheEntry [Rupee]",
        SecondLevelCacheDemo.dump(emf, Currency.class.getName(), Currency.class.getName() + "#1"));
  }

  @Test
  void collectionCacheAvoidsTheJoinQuery() {
    startEmpty();
    emf.runInTransaction(em -> em.find(Country.class, indiaId));   // country cached
    stats.clear();

    Set<String> first = emf.callInTransaction(em -> em.find(Country.class, indiaId).currencyNames());
    assertEquals(Set.of("Rupee"), first);
    assertEquals(1, stats.getPrepareStatementCount());               // collection SELECT
    assertEquals(1, stats.getSecondLevelCacheHitCount());            // country
    assertEquals(1, stats.getSecondLevelCacheMissCount());           // collection
    assertEquals(2, stats.getSecondLevelCachePutCount());            // collection + Rupee

    Set<String> second = emf.callInTransaction(em -> em.find(Country.class, indiaId).currencyNames());
    assertEquals(Set.of("Rupee"), second);
    assertEquals(1, stats.getPrepareStatementCount());               // no new SQL
    assertEquals(4, stats.getSecondLevelCacheHitCount());            // country + collection + Rupee
  }

  @Test
  void evictedElementEntityIsLoadedAgainById() {
    startEmpty();
    emf.runInTransaction(em -> em.find(Country.class, indiaId).getCurrencies().size());
    emf.runInTransaction(em -> em.find(Country.class, indiaId).getCurrencies().size()); // all cached
    emf.getCache().evict(Currency.class);
    stats.clear();

    emf.runInTransaction(em -> em.find(Country.class, indiaId).currencyNames());
    assertEquals(1, stats.getPrepareStatementCount());     // select ... from Currency where id=?
    assertEquals(1, stats.getEntityLoadCount());
  }

  @Test
  void queryCacheReturnsResultsWithoutSql() {
    startEmpty();
    List<Country> first = SecondLevelCacheDemo.euroCountries(emf);
    assertEquals("[France (Paris), Germany (Berlin)]", first.toString());
    assertEquals(1, stats.getQueryCacheMissCount());
    assertEquals(1, stats.getQueryCachePutCount());
    assertEquals(1, stats.getPrepareStatementCount());

    List<Country> second = SecondLevelCacheDemo.euroCountries(emf);
    assertEquals(first.toString(), second.toString());
    assertEquals(1, stats.getQueryCacheHitCount());
    assertEquals(1, stats.getPrepareStatementCount());
  }

  @Test
  void queryWithoutHintIsNotCached() {
    startEmpty();
    for (int i = 0; i < 2; i++) {
      emf.runInTransaction(em -> em.createQuery(SecondLevelCacheDemo.EURO_COUNTRIES, Country.class)
          .setParameter("currency", "Euro").getResultList());
    }
    assertEquals(0, stats.getQueryCacheHitCount());
    assertEquals(2, stats.getPrepareStatementCount());
  }

  @Test
  void updateOfAnyCountryInvalidatesTheCachedQuery() {
    startEmpty();
    SecondLevelCacheDemo.euroCountries(emf);
    emf.runInTransaction(em -> em.find(Country.class, indiaId).setCapital("Delhi"));  // India is not in the result
    long before = stats.getPrepareStatementCount();
    SecondLevelCacheDemo.euroCountries(emf);
    assertEquals(before + 1, stats.getPrepareStatementCount());
    assertEquals(2, stats.getQueryCacheMissCount());
    assertEquals(0, stats.getQueryCacheHitCount());
  }

  @Test
  void updateThroughHibernateRefreshesTheEntry() {
    startEmpty();
    findCapital();
    emf.runInTransaction(em -> em.find(Country.class, indiaId).setCapital("Delhi"));
    long statements = stats.getPrepareStatementCount();
    assertEquals("Delhi", findCapital());
    assertEquals(statements, stats.getPrepareStatementCount());     // served from the cache
  }

  @Test
  void changeOutsideHibernateIsStaleUntilEvicted() throws Exception {
    startEmpty();
    findCapital();
    try (Connection con = DriverManager.getConnection(Database.URL, "sa", "");
         Statement st = con.createStatement()) {
      st.executeUpdate("update Country set capital = 'Delhi' where name = 'India'");
    }
    assertEquals("New Delhi", findCapital());          // stale

    String bypass = emf.callInTransaction(em ->
        em.find(Country.class, indiaId, CacheRetrieveMode.BYPASS).getCapital());
    assertEquals("Delhi", bypass);
    assertEquals("New Delhi", findCapital());          // BYPASS does not replace the entry

    emf.getCache().evict(Country.class, indiaId);
    assertFalse(emf.getCache().contains(Country.class, indiaId));
    assertEquals("Delhi", findCapital());
  }

  @Test
  void evictAllKeepsCollectionsAndQueriesButEvictAllRegionsClearsThem() {
    SecondLevelCacheDemo.euroCountries(emf);
    emf.runInTransaction(em -> em.find(Country.class, indiaId).getCurrencies().size());
    org.hibernate.Cache hibernateCache = emf.unwrap(SessionFactory.class).getCache();
    assertTrue(hibernateCache.containsCollection(Country.class.getName() + ".currencies", indiaId));

    // Jakarta Persistence evictAll() removes entity data only
    emf.getCache().evictAll();
    assertFalse(emf.getCache().contains(Country.class, indiaId));
    assertTrue(hibernateCache.containsCollection(Country.class.getName() + ".currencies", indiaId));
    stats.clear();
    SecondLevelCacheDemo.euroCountries(emf);
    assertEquals(1, stats.getQueryCacheHitCount());

    // Hibernate evictAllRegions() also removes collections and query results
    hibernateCache.evictAllRegions();
    assertFalse(hibernateCache.containsCollection(Country.class.getName() + ".currencies", indiaId));
    stats.clear();
    SecondLevelCacheDemo.euroCountries(emf);
    assertEquals(1, stats.getQueryCacheMissCount());
  }

  @Test
  void evictionApis() {
    var cache = emf.getCache();
    cache.evict(Country.class, indiaId);
    assertFalse(cache.contains(Country.class, indiaId));
    assertTrue(cache.contains(Country.class, indiaId + 1));
    cache.evict(Country.class);
    assertFalse(cache.contains(Country.class, indiaId + 1));
    assertTrue(cache.contains(Currency.class, 1L));
    cache.evictAll();
    assertFalse(cache.contains(Currency.class, 1L));

    org.hibernate.Cache hibernateCache = emf.unwrap(SessionFactory.class).getCache();
    SecondLevelCacheDemo.euroCountries(emf);
    emf.runInTransaction(em -> em.find(Country.class, indiaId).getCurrencies().size());
    assertTrue(hibernateCache.containsCollection(Country.class.getName() + ".currencies", indiaId));
    hibernateCache.evictCollectionData(Country.class.getName() + ".currencies", indiaId);
    assertFalse(hibernateCache.containsCollection(Country.class.getName() + ".currencies", indiaId));
    hibernateCache.evictDefaultQueryRegion();
    stats.clear();
    SecondLevelCacheDemo.euroCountries(emf);
    assertEquals(1, stats.getQueryCacheMissCount());
  }

  @Test
  void immutableReadOnlyEntityIgnoresChanges() {
    stats.clear();
    emf.runInTransaction(em -> em.find(Currency.class, 1L).setName("Indian Rupee"));
    assertEquals(0, stats.getEntityUpdateCount());
    assertEquals("Rupee", emf.callInTransaction(em -> em.find(Currency.class, 1L).getName()));
  }

  @Test
  void readOnlyWithoutImmutableFailsOnUpdate() {
    Long id = emf.callInTransaction(em -> {
      Timezone zone = new Timezone("Asia/Kolkata");
      em.persist(zone);
      return zone.id;
    });
    RollbackException e = assertThrows(RollbackException.class,
        () -> emf.runInTransaction(em -> em.find(Timezone.class, id).name = "Asia/Calcutta"));
    assertEquals("Error while committing the transaction [Can't update read-only object]", e.getMessage());
    assertEquals("Asia/Kolkata", emf.callInTransaction(em -> em.find(Timezone.class, id).name));
  }

  @Test
  void readOnlyEntityCanBeDeleted() {
    Long yenId = emf.callInTransaction(em -> em.createQuery("select c.id from Currency c where c.name = 'Yen'", Long.class)
        .getSingleResult());
    emf.runInTransaction(em -> {
      em.createNativeQuery("delete from country_currency where currency_id = " + yenId).executeUpdate();
    });
    emf.runInTransaction(em -> em.remove(em.find(Currency.class, yenId)));
    assertFalse(emf.getCache().contains(Currency.class, yenId));
  }

  @Test
  void cacheableAloneOrCacheAloneBothCacheTheEntity() {
    Long[] ids = emf.callInTransaction(em -> {
      Language hindi = new Language("Hindi");
      Continent asia = new Continent("Asia");
      Ocean indian = new Ocean("Indian");
      em.persist(hindi);
      em.persist(asia);
      em.persist(indian);
      return new Long[] {hindi.id, asia.id, indian.id};
    });
    assertTrue(emf.getCache().contains(Language.class, ids[0]));
    assertTrue(emf.getCache().contains(Continent.class, ids[1]));
    assertFalse(emf.getCache().contains(Ocean.class, ids[2]));
  }

  @Test
  void nonstrictReadWriteRemovesTheEntryOnUpdate() {
    Long id = emf.callInTransaction(em -> {
      Landmark landmark = new Landmark("Taj Mahal");
      em.persist(landmark);
      return landmark.id;
    });
    assertFalse(emf.getCache().contains(Landmark.class, id));        // not put on insert
    emf.runInTransaction(em -> em.find(Landmark.class, id));
    assertTrue(emf.getCache().contains(Landmark.class, id));         // put on first read
    emf.runInTransaction(em -> em.find(Landmark.class, id).name = "Red Fort");
    assertFalse(emf.getCache().contains(Landmark.class, id));        // removed on update
    stats.clear();
    assertEquals("Red Fort", emf.callInTransaction(em -> em.find(Landmark.class, id).name));
    assertEquals(1, stats.getPrepareStatementCount());
  }

  @Test
  void bulkJpqlUpdateEvictsTheWholeEntityRegion() {
    assertTrue(emf.getCache().contains(Country.class, indiaId + 1));
    emf.runInTransaction(em -> em.createQuery("update Country set capital = 'Delhi' where id = :id")
        .setParameter("id", indiaId).executeUpdate());
    assertFalse(emf.getCache().contains(Country.class, indiaId + 1));   // Germany evicted too
    assertTrue(emf.getCache().contains(Currency.class, 1L));            // other regions stay
  }

  @Test
  void nativeUpdateEvictsAllRegions() {
    emf.runInTransaction(em -> em.createNativeQuery("update Country set capital = 'Delhi' where id = " + indiaId)
        .executeUpdate());
    assertFalse(emf.getCache().contains(Country.class, indiaId + 1));
    assertFalse(emf.getCache().contains(Currency.class, 1L));
  }

  @Test
  void regionStatistics() {
    startEmpty();
    findCapital();
    findCapital();
    findCapital();
    var region = stats.getDomainDataRegionStatistics(Country.class.getName());
    assertEquals(2, region.getHitCount());
    assertEquals(1, region.getMissCount());
    assertEquals(1, region.getPutCount());
  }
}
