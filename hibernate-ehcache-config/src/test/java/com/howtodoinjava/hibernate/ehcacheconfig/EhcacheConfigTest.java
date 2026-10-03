package com.howtodoinjava.hibernate.ehcacheconfig;

import static com.howtodoinjava.hibernate.ehcacheconfig.Database.pause;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.hibernate.cache.CacheException;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;

class EhcacheConfigTest {

  private static final String LANGUAGE = Language.class.getName();
  private static final String COLLECTION = LANGUAGE + ".translations";
  private static final String UNLIMITED = "heap=" + Long.MAX_VALUE + " entries";

  // ------------------------------------------------------------ regions and ehcache.xml

  @Test
  void hibernateCreatesOneCachePerRegion() {
    try (EntityManagerFactory emf = Database.create(false)) {
      assertEquals(List.of(LANGUAGE, COLLECTION, "default-query-results-region",
          "default-update-timestamps-region", "language-queries", "translations"), Database.cacheNames(emf));
      assertEquals(Set.of(LANGUAGE, COLLECTION, "translations", "default-query-results-region"),
          Set.of(Database.statistics(emf).getSecondLevelCacheRegionNames()));
    }
  }

  @Test
  void templateSettingsAndOverrides() {
    try (EntityManagerFactory emf = Database.create(false)) {
      assertEquals("heap=500 entries", Database.resources(emf, LANGUAGE));
      assertEquals("TTL of PT30M", Database.expiry(emf, LANGUAGE));
      assertEquals("heap=500 entries", Database.resources(emf, COLLECTION));
      assertEquals("heap=5000 entries", Database.resources(emf, "translations"));
      assertEquals("TTI of PT10M", Database.expiry(emf, "translations"));
      assertEquals("heap=200 entries", Database.resources(emf, "default-query-results-region"));
      assertEquals("TTL of PT5M", Database.expiry(emf, "default-query-results-region"));
      assertEquals("No Expiry", Database.expiry(emf, "default-update-timestamps-region"));
    }
  }

  @Test
  void insertPutsEntitiesIntoTheirRegions() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Statistics stats = Database.statistics(emf);
      EhcacheConfigDemo.saveLanguages(emf);
      assertEquals(3, stats.getDomainDataRegionStatistics(LANGUAGE).getPutCount());
      assertEquals(3, stats.getDomainDataRegionStatistics("translations").getPutCount());
      assertEquals(0, stats.getDomainDataRegionStatistics(COLLECTION).getPutCount());
      assertEquals(3, Database.entryCount(emf, LANGUAGE));
      assertEquals(Long.MIN_VALUE, stats.getDomainDataRegionStatistics(LANGUAGE).getElementCountInMemory());
    }
  }

  @Test
  void perRegionStatisticsAfterTwoSessions() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Statistics stats = Database.statistics(emf);
      Long enId = EhcacheConfigDemo.saveLanguages(emf)[0];
      stats.clear();
      emf.runInTransaction(em -> assertEquals("[greeting=Hello]", em.find(Language.class, enId).getTranslations().toString()));
      emf.runInTransaction(em -> assertEquals("[greeting=Hello]", em.find(Language.class, enId).getTranslations().toString()));

      assertCounts(stats.getDomainDataRegionStatistics(LANGUAGE), 2, 0, 0);
      assertCounts(stats.getDomainDataRegionStatistics(COLLECTION), 1, 1, 1);
      assertCounts(stats.getDomainDataRegionStatistics("translations"), 1, 0, 0);
      assertEquals(1, stats.getPrepareStatementCount());
    }
  }

  @Test
  void queryResultsGoToTheNamedRegion() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Statistics stats = Database.statistics(emf);
      EhcacheConfigDemo.saveLanguages(emf);
      stats.clear();
      assertEquals("[de (German), en (English), fr (French)]", EhcacheConfigDemo.languagesByCode(emf).toString());
      assertEquals("[de (German), en (English), fr (French)]", EhcacheConfigDemo.languagesByCode(emf).toString());
      assertCounts(stats.getQueryRegionStatistics("language-queries"), 1, 1, 1);
      assertCounts(stats.getCacheRegionStatistics("language-queries"), 1, 1, 1);
      assertCounts(stats.getCacheRegionStatistics("default-query-results-region"), 0, 0, 0);
      assertTrue(Set.of(stats.getSecondLevelCacheRegionNames()).contains("language-queries"));
      assertEquals(1, stats.getPrepareStatementCount());
    }
  }

  @Test
  void domainRegionStatisticsRejectQueryRegion() {
    try (EntityManagerFactory emf = Database.create(false)) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> Database.statistics(emf).getDomainDataRegionStatistics("default-query-results-region"));
      assertEquals("Region name [default-query-results-region] referred to a query result region, not a domain data region",
          e.getMessage());
    }
  }

  // ------------------------------------------------------------ expiry

  @Test
  void timeToLiveRemovesTheEntryOneSecondAfterThePut() {
    try (EntityManagerFactory emf = Database.create("ehcache-expiry.xml", Map.of(), false)) {
      Statistics stats = Database.statistics(emf);
      Long enId = EhcacheConfigDemo.saveLanguages(emf)[0];
      stats.clear();
      emf.runInTransaction(em -> em.find(Language.class, enId));
      assertCounts(stats.getDomainDataRegionStatistics(LANGUAGE), 1, 0, 0);
      pause(Duration.ofMillis(500));
      emf.runInTransaction(em -> em.find(Language.class, enId));
      assertCounts(stats.getDomainDataRegionStatistics(LANGUAGE), 2, 0, 0);
      pause(Duration.ofMillis(700));
      emf.runInTransaction(em -> em.find(Language.class, enId));
      assertCounts(stats.getDomainDataRegionStatistics(LANGUAGE), 2, 1, 1);
    }
  }

  @Test
  void timeToIdleKeepsAnEntryThatIsReadOften() {
    try (EntityManagerFactory emf = Database.create("ehcache-expiry.xml", Map.of(), false)) {
      Statistics stats = Database.statistics(emf);
      Long helloId = EhcacheConfigDemo.saveLanguages(emf)[3];
      assertEquals("TTI of PT2S", Database.expiry(emf, "translations"));
      assertEquals("TTL of PT1S", Database.expiry(emf, LANGUAGE));
      emf.getCache().evict(Translation.class);
      stats.clear();
      emf.runInTransaction(em -> em.find(Translation.class, helloId));
      assertCounts(stats.getDomainDataRegionStatistics("translations"), 0, 1, 1);
      pause(Duration.ofMillis(1200));
      emf.runInTransaction(em -> em.find(Translation.class, helloId));
      pause(Duration.ofMillis(1200));
      emf.runInTransaction(em -> em.find(Translation.class, helloId));
      // 2.4 seconds after the put, still a hit: each read resets the idle time
      assertCounts(stats.getDomainDataRegionStatistics("translations"), 2, 1, 1);
      pause(Duration.ofMillis(2200));
      emf.runInTransaction(em -> em.find(Translation.class, helloId));
      assertCounts(stats.getDomainDataRegionStatistics("translations"), 2, 2, 2);
    }
  }

  // ------------------------------------------------------------ sizing and eviction

  @Test
  void fullHeapEvictsAnEntry() {
    try (EntityManagerFactory emf = Database.create("ehcache-small-heap.xml", Map.of(), false)) {
      Statistics stats = Database.statistics(emf);
      Long[] ids = EhcacheConfigDemo.saveLanguages(emf);
      assertEquals("heap=2 entries", Database.resources(emf, LANGUAGE));
      assertEquals(3, stats.getDomainDataRegionStatistics(LANGUAGE).getPutCount());
      assertEquals(2, Database.entryCount(emf, LANGUAGE));

      Long evicted = null;
      int cached = 0;
      for (int i = 0; i < 3; i++) {
        if (emf.getCache().contains(Language.class, ids[i])) {
          cached++;
        } else {
          evicted = ids[i];
        }
      }
      assertEquals(2, cached);
      Long id = evicted;
      stats.clear();
      emf.runInTransaction(em -> em.find(Language.class, id));
      assertCounts(stats.getDomainDataRegionStatistics(LANGUAGE), 0, 1, 1);
      assertEquals(1, stats.getPrepareStatementCount());

      // loading it again put it back and pushed another language out
      assertTrue(emf.getCache().contains(Language.class, id));
      assertEquals(2, Database.entryCount(emf, LANGUAGE));
      int nowCached = 0;
      for (int i = 0; i < 3; i++) {
        nowCached += emf.getCache().contains(Language.class, ids[i]) ? 1 : 0;
      }
      assertEquals(2, nowCached);
    }
  }

  @Test
  void offheapKeepsWhatDoesNotFitOnTheHeap() {
    try (EntityManagerFactory emf = Database.create("ehcache-small-heap.xml", Map.of(), false)) {
      Statistics stats = Database.statistics(emf);
      Long[] ids = EhcacheConfigDemo.saveLanguages(emf);
      assertEquals("heap=2 entries, offheap=10 mb", Database.resources(emf, "translations"));
      assertEquals(3, Database.entryCount(emf, "translations"));
      stats.clear();
      List<String> texts = emf.callInTransaction(em -> List.of(
          em.find(Translation.class, ids[3]).toString(),
          em.find(Translation.class, ids[4]).toString(),
          em.find(Translation.class, ids[5]).toString()));
      assertEquals(List.of("greeting=Hello", "greeting=Bonjour", "greeting=Hallo"), texts);
      assertCounts(stats.getDomainDataRegionStatistics("translations"), 3, 0, 0);
      assertEquals(0, stats.getPrepareStatementCount());
    }
  }

  @Test
  void sizeBasedHeapHoldsFewerEntriesAndWarnsOnJava25() {
    String log = captureStdErr(() -> {
      try (EntityManagerFactory emf = Database.create("ehcache-size.xml", Map.of(), false)) {
        assertEquals("heap=64 kb", Database.resources(emf, LANGUAGE));
        emf.runInTransaction(em -> {
          for (int i = 1; i <= 1000; i++) {
            em.persist(new Language("l" + i, "Language " + i));
          }
        });
        int entries = Database.entryCount(emf, LANGUAGE);
        assertTrue(entries > 0 && entries < 1000, "entries=" + entries);
      }
    });
    assertTrue(log.contains("The JVM is preventing Ehcache from accessing the subgraph beneath"
        + " 'private final byte[] java.lang.String.value' - cache sizes may be underestimated as a result"), log);
  }

  // ------------------------------------------------------------ missing_cache_strategy

  @Test
  void createWarnIsTheDefaultAndLogsEveryMissingRegion() {
    String log = captureStdErr(() -> {
      try (EntityManagerFactory emf = Database.create("ehcache-missing.xml", Map.of(), false)) {
        assertEquals(UNLIMITED, Database.resources(emf, "translations"));
        assertEquals(UNLIMITED, Database.resources(emf, COLLECTION));
      }
    });
    assertTrue(log.contains("HHH90001006: Missing cache region [translations] was created with provider-specific"
        + " default policies. Explicitly configure the region and its policies, or disable this warning by setting"
        + " 'hibernate.javax.cache.missing_cache_strategy' to 'create'."), log);
    assertTrue(log.contains("HHH90001006: Missing cache region [" + COLLECTION + "]"), log);
  }

  @Test
  void createCreatesTheCacheSilently() {
    String log = captureStdErr(() -> {
      try (EntityManagerFactory emf = Database.create("ehcache-missing.xml",
          Map.of("hibernate.javax.cache.missing_cache_strategy", "create"), false)) {
        assertEquals(UNLIMITED, Database.resources(emf, "translations"));
      }
    });
    assertFalse(log.contains("HHH90001006"), log);
  }

  @Test
  void failStopsTheStartup() {
    PersistenceException e = assertThrows(PersistenceException.class, () -> Database.create("ehcache-missing.xml",
        Map.of("hibernate.javax.cache.missing_cache_strategy", "fail"), false));
    assertEquals("Unable to build Hibernate SessionFactory  [persistence unit: ehcache-config] ", e.getMessage());
    Throwable cause = rootCause(e);
    assertInstanceOf(CacheException.class, cause);
    // the first missing region Hibernate builds stops the startup: translations or the collection region
    assertTrue(Set.of("On-the-fly creation of JCache Cache objects is not supported [translations]",
            "On-the-fly creation of JCache Cache objects is not supported [" + COLLECTION + "]")
        .contains(cause.getMessage()), cause.getMessage());
  }

  @Test
  void defaultTemplateConfiguresRegionsWithoutACacheElement() {
    String log = captureStdErr(() -> {
      try (EntityManagerFactory emf = Database.create("ehcache-default-template.xml", Map.of(), false)) {
        assertEquals("heap=500 entries", Database.resources(emf, LANGUAGE));
        assertEquals("No Expiry", Database.expiry(emf, LANGUAGE));
        assertEquals("heap=1000 entries", Database.resources(emf, "translations"));
        assertEquals("TTL of PT10M", Database.expiry(emf, "translations"));
        // the timestamps region gets the template too, including its TTL
        assertEquals("TTL of PT10M", Database.expiry(emf, "default-update-timestamps-region"));
      }
    });
    // Hibernate still reports the region as missing, because no cache existed before startup
    assertTrue(log.contains("HHH90001006: Missing cache region [translations]"), log);
  }

  @Test
  void withoutTheUriEveryRegionIsMissing() {
    String log = captureStdErr(() -> {
      try (EntityManagerFactory emf = Database.create(null, Map.of(), false)) {
        assertEquals(UNLIMITED, Database.resources(emf, LANGUAGE));
        assertEquals(UNLIMITED, Database.resources(emf, "default-update-timestamps-region"));
      }
    });
    for (String region : List.of(LANGUAGE, COLLECTION, "translations", "default-query-results-region",
        "default-update-timestamps-region")) {
      assertTrue(log.contains("HHH90001006: Missing cache region [" + region + "]"), log);
    }
  }

  @Test
  void regionPrefixChangesTheCacheNamesButNotTheStatisticsNames() {
    try (EntityManagerFactory emf = Database.create("ehcache.xml",
        Map.of("hibernate.cache.region_prefix", "i18n", "hibernate.javax.cache.missing_cache_strategy", "create"), false)) {
      List<String> names = Database.cacheNames(emf);
      assertTrue(names.containsAll(List.of("i18n." + LANGUAGE, "i18n." + COLLECTION, "i18n.translations",
          "i18n.default-query-results-region", "i18n.default-update-timestamps-region")), names.toString());
      assertEquals(UNLIMITED, Database.resources(emf, "i18n." + LANGUAGE));
      assertEquals(Set.of(LANGUAGE, COLLECTION, "translations", "default-query-results-region"),
          Set.of(Database.statistics(emf).getSecondLevelCacheRegionNames()));
    }
  }

  @Test
  void typedCacheRejectsHibernateKeys() {
    try (EntityManagerFactory emf = Database.create("ehcache-typed.xml", Map.of(), false)) {
      RollbackException e = assertThrows(RollbackException.class, () -> EhcacheConfigDemo.saveLanguages(emf));
      assertEquals("Error while committing the transaction [Unable to perform afterTransactionCompletion callback:"
          + " Invalid key type, expected : java.lang.Long but was : org.hibernate.cache.internal.BasicCacheKeyImplementation]",
          e.getMessage());
      assertInstanceOf(ClassCastException.class, rootCause(e));
      // the error comes after the database commit: the rows are saved
      Long rows = emf.callInTransaction(em -> em.createQuery("select count(*) from Language", Long.class).getSingleResult());
      assertEquals(3L, rows);
    }
  }

  // ------------------------------------------------------------ concurrency strategies

  @Test
  void readOnlyCachesOnInsertAndRejectsUpdates() {
    String log = captureStdErr(() -> {
      try (EntityManagerFactory emf = labels()) {
        Statistics stats = Database.statistics(emf);
        Long id = insert(emf, ReadOnlyLabel::new);
        assertEquals(1, stats.getDomainDataRegionStatistics(ReadOnlyLabel.class.getName()).getPutCount());
        RollbackException e = assertThrows(RollbackException.class,
            () -> emf.runInTransaction(em -> em.find(ReadOnlyLabel.class, id).setText("Save changes")));
        assertEquals("Error while committing the transaction [Can't update read-only object]", e.getMessage());
        stats.clear();
        assertEquals("Save", emf.callInTransaction(em -> em.find(ReadOnlyLabel.class, id).getText()));
        assertCounts(stats.getDomainDataRegionStatistics(ReadOnlyLabel.class.getName()), 0, 1, 1);
        emf.runInTransaction(em -> em.remove(em.find(ReadOnlyLabel.class, id)));
        assertFalse(emf.getCache().contains(ReadOnlyLabel.class, id));
      }
    });
    assertTrue(log.contains("HHH90001003: Read-only caching was requested for mutable entity ["
        + ReadOnlyLabel.class.getName() + "]"), log);
  }

  @Test
  void nonstrictReadWriteSkipsInsertAndRemovesOnUpdate() {
    try (EntityManagerFactory emf = labels()) {
      Statistics stats = Database.statistics(emf);
      Long id = insert(emf, NonstrictLabel::new);
      assertEquals(0, stats.getDomainDataRegionStatistics(NonstrictLabel.class.getName()).getPutCount());
      assertFalse(emf.getCache().contains(NonstrictLabel.class, id));
      emf.runInTransaction(em -> em.find(NonstrictLabel.class, id).setText("Save changes"));
      assertFalse(emf.getCache().contains(NonstrictLabel.class, id));
      stats.clear();
      assertEquals("Save changes", emf.callInTransaction(em -> em.find(NonstrictLabel.class, id).getText()));
      assertCounts(stats.getDomainDataRegionStatistics(NonstrictLabel.class.getName()), 0, 1, 1);
    }
  }

  @Test
  void readWriteReplacesTheEntryAtCommit() {
    try (EntityManagerFactory emf = labels()) {
      Statistics stats = Database.statistics(emf);
      Long id = insert(emf, ReadWriteLabel::new);
      assertEquals(1, stats.getDomainDataRegionStatistics(ReadWriteLabel.class.getName()).getPutCount());
      stats.clear();
      emf.runInTransaction(em -> em.find(ReadWriteLabel.class, id).setText("Save changes"));
      assertEquals(1, stats.getDomainDataRegionStatistics(ReadWriteLabel.class.getName()).getPutCount());
      stats.clear();
      assertEquals("Save changes", emf.callInTransaction(em -> em.find(ReadWriteLabel.class, id).getText()));
      assertCounts(stats.getDomainDataRegionStatistics(ReadWriteLabel.class.getName()), 1, 0, 0);
      assertEquals(0, stats.getPrepareStatementCount());
    }
  }

  @Test
  void transactionalWarnsWithJCacheAndBehavesLikeReadWrite() {
    String log = captureStdErr(() -> {
      try (EntityManagerFactory emf = labels()) {
        Statistics stats = Database.statistics(emf);
        Long id = insert(emf, TransactionalLabel::new);
        assertEquals(1, stats.getDomainDataRegionStatistics(TransactionalLabel.class.getName()).getPutCount());
        emf.runInTransaction(em -> em.find(TransactionalLabel.class, id).setText("Save changes"));
        stats.clear();
        assertEquals("Save changes", emf.callInTransaction(em -> em.find(TransactionalLabel.class, id).getText()));
        assertCounts(stats.getDomainDataRegionStatistics(TransactionalLabel.class.getName()), 1, 0, 0);
      }
    });
    assertTrue(log.contains("HHH90001008: Cache region [" + TransactionalLabel.class.getName()
        + "] has the access type 'transactional' which is not supported by [JCacheRegionFactory]."
        + " Ensure cache implementation supports JTA transactions."), log);
  }

  // ------------------------------------------------------------ helpers

  private static EntityManagerFactory labels() {
    return Database.create("ehcache-labels.xml", Map.of(), false,
        ReadOnlyLabel.class, NonstrictLabel.class, ReadWriteLabel.class, TransactionalLabel.class);
  }

  private static <T extends Label> Long insert(EntityManagerFactory emf, Function<String, T> factory) {
    return emf.callInTransaction(em -> {
      T label = factory.apply("Save");
      em.persist(label);
      return label.getId();
    });
  }

  private static void assertCounts(CacheRegionStatistics region, long hits, long misses, long puts) {
    assertEquals(List.of(hits, misses, puts),
        List.of(region.getHitCount(), region.getMissCount(), region.getPutCount()),
        region.getRegionName() + " [hits, misses, puts]");
  }

  private static Throwable rootCause(Throwable e) {
    Throwable cause = e;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause;
  }

  private static String captureStdErr(Runnable action) {
    PrintStream original = System.err;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    System.setErr(new PrintStream(buffer, true));
    try {
      action.run();
    } finally {
      System.setErr(original);
    }
    return buffer.toString();
  }
}
