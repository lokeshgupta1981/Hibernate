package com.howtodoinjava.hibernate.ehcache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;

class EhcacheSetupTest {

  private static String rootMessage(Throwable e) {
    Throwable t = e;
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t.getClass().getName() + ": " + t.getMessage();
  }

  private static String allMessages(Throwable e) {
    StringBuilder sb = new StringBuilder();
    for (Throwable t = e; t != null; t = t.getCause()) {
      sb.append(t.getClass().getName()).append(": ").append(t.getMessage()).append('\n');
    }
    return sb.toString();
  }

  @Test
  void savingFillsTheCache() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Statistics stats = Database.statistics(emf);
      Database.saveSampleData(emf);
      assertEquals(4, stats.getSecondLevelCachePutCount());
      assertEquals(3, stats.getDomainDataRegionStatistics(Airport.class.getName()).getPutCount());
      assertEquals(7, stats.getPrepareStatementCount());
      assertTrue(emf.getCache().contains(Airport.class, "DEL"));
    }
  }

  @Test
  void secondSessionReadsAirportFromCache() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Statistics stats = Database.statistics(emf);
      Database.saveSampleData(emf);
      emf.getCache().evictAll();
      stats.clear();
      assertFalse(emf.getCache().contains(Airport.class, "DEL"));

      Airport first = emf.callInTransaction(em -> em.find(Airport.class, "DEL"));
      assertEquals("Delhi", first.getCity());
      assertEquals(0, stats.getSecondLevelCacheHitCount());
      assertEquals(1, stats.getSecondLevelCacheMissCount());
      assertEquals(1, stats.getSecondLevelCachePutCount());
      assertEquals(1, stats.getPrepareStatementCount());

      Airport second = emf.callInTransaction(em -> em.find(Airport.class, "DEL"));
      assertEquals("Indira Gandhi", second.getName());
      assertEquals(1, stats.getSecondLevelCacheHitCount());
      assertEquals(1, stats.getSecondLevelCacheMissCount());
      assertEquals(1, stats.getSecondLevelCachePutCount());
      assertEquals(1, stats.getPrepareStatementCount());   // no new SQL
    }
  }

  @Test
  void hubsCollectionIsCached() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Statistics stats = Database.statistics(emf);
      Long id = Database.saveSampleData(emf);
      emf.getCache().evictAll();
      emf.runInTransaction(em -> em.find(Airport.class, "DEL"));
      stats.clear();

      Set<String> first = emf.callInTransaction(em -> em.find(Airline.class, id).hubCities());
      assertEquals(Set.of("Delhi", "Mumbai"), first);
      assertEquals(0, stats.getSecondLevelCacheHitCount());
      assertEquals(2, stats.getSecondLevelCacheMissCount());
      assertEquals(3, stats.getSecondLevelCachePutCount());
      assertEquals(2, stats.getPrepareStatementCount());

      Set<String> second = emf.callInTransaction(em -> em.find(Airline.class, id).hubCities());
      assertEquals(first, second);
      assertEquals(4, stats.getSecondLevelCacheHitCount());
      assertEquals(2, stats.getPrepareStatementCount());
      assertEquals(1, stats.getCollectionStatistics(Airline.class.getName() + ".hubs").getCacheHitCount());
    }
  }

  @Test
  void cachedQueryRunsOnce() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Statistics stats = Database.statistics(emf);
      Database.saveSampleData(emf);
      stats.clear();
      List<Airport> first = emf.callInTransaction(em -> EhcacheDemo.airportsIn(em, "Mumbai"));
      List<Airport> second = emf.callInTransaction(em -> EhcacheDemo.airportsIn(em, "Mumbai"));
      assertEquals("BOM", first.get(0).getIataCode());
      assertEquals("BOM", second.get(0).getIataCode());
      assertEquals(1, stats.getQueryCacheMissCount());
      assertEquals(1, stats.getQueryCachePutCount());
      assertEquals(1, stats.getQueryCacheHitCount());
      assertEquals(1, stats.getPrepareStatementCount());
    }
  }

  @Test
  void firstReadAfterInsertIsAHit() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Statistics stats = Database.statistics(emf);
      Database.saveSampleData(emf);
      stats.clear();
      emf.runInTransaction(em -> em.find(Airport.class, "DEL"));
      assertEquals(1, stats.getSecondLevelCacheHitCount());
      assertEquals(0, stats.getPrepareStatementCount());
    }
  }

  @Test
  void updatingTheTableInvalidatesCachedQuery() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Statistics stats = Database.statistics(emf);
      Database.saveSampleData(emf);
      emf.runInTransaction(em -> EhcacheDemo.airportsIn(em, "Mumbai"));
      emf.runInTransaction(em -> em.find(Airport.class, "DEL").setName("IGI Airport"));
      stats.clear();
      emf.runInTransaction(em -> EhcacheDemo.airportsIn(em, "Mumbai"));
      assertEquals(0, stats.getQueryCacheHitCount());
      assertEquals(1, stats.getPrepareStatementCount());
    }
  }

  @Test
  void queryWithoutHintAlwaysRunsSql() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Statistics stats = Database.statistics(emf);
      Database.saveSampleData(emf);
      stats.clear();
      for (int i = 0; i < 2; i++) {
        emf.runInTransaction(em -> em.createQuery(
                "select a from Airport a where a.city = :city", Airport.class)
            .setParameter("city", "Mumbai").getResultList());
      }
      assertEquals(2, stats.getPrepareStatementCount());
      assertEquals(0, stats.getQueryCacheHitCount());
    }
  }

  @Test
  void regionsCreatedInEhcache() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Set<String> regions = Set.copyOf(Arrays.asList(Database.statistics(emf).getSecondLevelCacheRegionNames()));
      assertEquals(Set.of(
          "com.howtodoinjava.hibernate.ehcache.Airport",
          "com.howtodoinjava.hibernate.ehcache.Airline",
          "com.howtodoinjava.hibernate.ehcache.Airline.hubs",
          "default-query-results-region"), regions);
    }
  }

  @Test
  void wrongConfigUriFailsAtStartup() {
    RuntimeException e = assertThrows(RuntimeException.class,
        () -> Database.create(false, Map.of("hibernate.javax.cache.uri", "ehcache-missing.xml")));
    assertEquals("org.hibernate.cache.CacheException: Couldn't load URI from ehcache-missing.xml", rootMessage(e));
    assertTrue(allMessages(e).contains("Cache provider not started"));
  }

  @Test
  void wrongProviderClassFailsAtStartup() {
    RuntimeException e = assertThrows(RuntimeException.class,
        () -> Database.create(false, Map.of("hibernate.javax.cache.provider", "org.ehcache.EhcacheProvider")));
    assertTrue(allMessages(e).contains(
        "javax.cache.CacheException: Failed to load the CachingProvider [org.ehcache.EhcacheProvider]"));
  }

  @Test
  void missingRegionFailsWithFailStrategy() {
    RuntimeException e = assertThrows(RuntimeException.class,
        () -> Database.create(false, Map.of("hibernate.javax.cache.uri", "ehcache-no-hubs.xml")));
    assertEquals("org.hibernate.cache.CacheException: On-the-fly creation of JCache Cache objects is not supported"
        + " [com.howtodoinjava.hibernate.ehcache.Airline.hubs]", rootMessage(e));
  }

  @Test
  void missingRegionIsCreatedWithCreateWarn() {
    try (EntityManagerFactory emf = Database.create(false, Map.of(
        "hibernate.javax.cache.uri", "ehcache-no-hubs.xml",
        "hibernate.javax.cache.missing_cache_strategy", "create-warn"))) {
      Long id = Database.saveSampleData(emf);
      emf.runInTransaction(em -> em.find(Airline.class, id).hubCities());
      Statistics stats = Database.statistics(emf);
      stats.clear();
      emf.runInTransaction(em -> em.find(Airline.class, id).hubCities());
      assertEquals(0, stats.getPrepareStatementCount());
    }
  }

  @Test
  void defaultMissingCacheStrategyIsCreateWarn() {
    Map<String, Object> overrides = new HashMap<>();
    overrides.put("hibernate.javax.cache.uri", "ehcache-no-hubs.xml");
    overrides.put("hibernate.javax.cache.missing_cache_strategy", null);
    try (EntityManagerFactory emf = Database.create(false, overrides)) {
      assertTrue(emf.isOpen());
    }
  }

  @Test
  void withoutUriTheRegionsAreMissing() {
    Map<String, Object> overrides = new HashMap<>();
    overrides.put("hibernate.javax.cache.uri", null);
    RuntimeException e = assertThrows(RuntimeException.class, () -> Database.create(false, overrides));
    assertEquals("org.hibernate.cache.CacheException: On-the-fly creation of JCache Cache objects is not supported"
        + " [default-update-timestamps-region]", rootMessage(e));
  }

  @Test
  void springStyleJcacheKeyIsIgnored() {
    Map<String, Object> overrides = new HashMap<>();
    overrides.put("hibernate.javax.cache.uri", null);
    overrides.put("hibernate.cache.jcache.uri", "ehcache.xml");
    RuntimeException e = assertThrows(RuntimeException.class, () -> Database.create(false, overrides));
    assertEquals("org.hibernate.cache.CacheException: On-the-fly creation of JCache Cache objects is not supported"
        + " [default-update-timestamps-region]", rootMessage(e));
  }

  @Test
  void unknownRegionFactoryNameFails() {
    RuntimeException e = assertThrows(RuntimeException.class,
        () -> Database.create(false, Map.of("hibernate.cache.region.factory_class", "ehcache")));
    assertTrue(allMessages(e).contains(
        "Unable to resolve name [ehcache] as strategy [org.hibernate.cache.spi.RegionFactory]"));
  }

  @Test
  void secondLevelCacheSwitchedOffCachesNothing() {
    try (EntityManagerFactory emf = Database.create(false,
        Map.of("hibernate.cache.use_second_level_cache", false))) {
      Statistics stats = Database.statistics(emf);
      Database.saveSampleData(emf);
      stats.clear();
      emf.runInTransaction(em -> em.find(Airport.class, "DEL"));
      emf.runInTransaction(em -> em.find(Airport.class, "DEL"));
      assertEquals(2, stats.getPrepareStatementCount());
      assertEquals(0, stats.getSecondLevelCacheHitCount());
      assertEquals(0, stats.getSecondLevelCachePutCount());
    }
  }

  @Test
  void regionFactoryIsDetectedWhenHibernateJcacheIsTheOnlyOne() {
    Map<String, Object> overrides = new HashMap<>();
    overrides.put("hibernate.cache.region.factory_class", null);
    overrides.put("hibernate.cache.use_second_level_cache", null);
    try (EntityManagerFactory emf = Database.create(false, overrides)) {
      Statistics stats = Database.statistics(emf);
      Database.saveSampleData(emf);
      emf.getCache().evictAll();
      stats.clear();
      emf.runInTransaction(em -> em.find(Airport.class, "DEL"));
      emf.runInTransaction(em -> em.find(Airport.class, "DEL"));
      assertEquals(1, stats.getPrepareStatementCount());
      assertEquals(1, stats.getSecondLevelCacheHitCount());
    }
  }

  @Test
  void useSecondLevelCacheDefaultsToTrue() {
    Map<String, Object> overrides = new HashMap<>();
    overrides.put("hibernate.cache.use_second_level_cache", null);
    try (EntityManagerFactory emf = Database.create(false, overrides)) {
      Statistics stats = Database.statistics(emf);
      Database.saveSampleData(emf);
      emf.getCache().evictAll();
      stats.clear();
      emf.runInTransaction(em -> em.find(Airport.class, "DEL"));
      emf.runInTransaction(em -> em.find(Airport.class, "DEL"));
      assertEquals(1, stats.getPrepareStatementCount());
      assertEquals(1, stats.getSecondLevelCacheHitCount());
    }
  }
}
