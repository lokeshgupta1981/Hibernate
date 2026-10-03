package com.howtodoinjava.hibernate.ehcacheconfig;

import static com.howtodoinjava.hibernate.ehcacheconfig.Database.pause;

import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;

public class EhcacheConfigDemo {

  private static final String LANGUAGE = Language.class.getName();

  public static void main(String[] args) {
    regionsAndStatistics();
    expiry();
    evictionAndOffheap();
    sizeBasedHeap();
    missingCacheStrategy();
    concurrencyStrategies();
    regionPrefixAndDefaultTemplate();
    typedCache();
  }

  static void typedCache() {
    step("15. key-type java.lang.Long on the Language cache");
    try (EntityManagerFactory emf = Database.create("ehcache-typed.xml", Map.of(), false)) {
      saveLanguages(emf);
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
      System.out.println("Caused by: " + rootCause(e));
    }
  }

  static void regionsAndStatistics() {
    try (EntityManagerFactory emf = Database.create(true)) {
      Statistics stats = Database.statistics(emf);

      step("1. Caches in ehcache.xml, one per region");
      for (String name : Database.cacheNames(emf)) {
        System.out.println(name + " -> " + Database.resources(emf, name) + ", " + Database.expiry(emf, name));
      }

      step("2. Save three languages with one translation each");
      Long[] ids = saveLanguages(emf);
      Long enId = ids[0];
      printRegion(stats, LANGUAGE);
      printRegion(stats, "translations");

      step("3. Read English and its translations in two new sessions");
      stats.clear();
      emf.runInTransaction(em -> System.out.println(em.find(Language.class, enId).getTranslations()));
      emf.runInTransaction(em -> System.out.println(em.find(Language.class, enId).getTranslations()));
      printRegion(stats, LANGUAGE);
      printRegion(stats, LANGUAGE + ".translations");
      printRegion(stats, "translations");

      step("4. A cached query in the named region language-queries, run twice");
      System.out.println(languagesByCode(emf));
      System.out.println(languagesByCode(emf));
      CacheRegionStatistics queries = stats.getQueryRegionStatistics("language-queries");
      System.out.println("language-queries: hits=" + queries.getHitCount() + " misses=" + queries.getMissCount()
          + " puts=" + queries.getPutCount());

      step("5. Statistics for every region");
      for (String region : stats.getSecondLevelCacheRegionNames()) {
        CacheRegionStatistics s = stats.getCacheRegionStatistics(region);
        System.out.println(region + ": hits=" + s.getHitCount() + " misses=" + s.getMissCount() + " puts=" + s.getPutCount());
      }
      System.out.println("element count of " + LANGUAGE + ": "
          + stats.getDomainDataRegionStatistics(LANGUAGE).getElementCountInMemory());
      System.out.println("entries in the Ehcache cache: " + Database.entryCount(emf, LANGUAGE));
    }
  }

  static void expiry() {
    try (EntityManagerFactory emf = Database.create("ehcache-expiry.xml", Map.of(), true)) {
      Statistics stats = Database.statistics(emf);
      Long[] ids = saveLanguages(emf);
      Long enId = ids[0];
      Long helloId = ids[3];

      step("6. TTL of 1 second on Language");
      stats.clear();
      long start = System.nanoTime();
      findLanguage(emf, stats, enId, start);
      pause(Duration.ofMillis(500));
      findLanguage(emf, stats, enId, start);
      pause(Duration.ofMillis(700));
      findLanguage(emf, stats, enId, start);

      step("7. TTI of 2 seconds on translations");
      stats.clear();
      emf.getCache().evict(Translation.class);
      start = System.nanoTime();
      findTranslation(emf, stats, helloId, start);
      pause(Duration.ofMillis(1200));
      findTranslation(emf, stats, helloId, start);
      pause(Duration.ofMillis(1200));
      findTranslation(emf, stats, helloId, start);
      pause(Duration.ofMillis(2200));
      findTranslation(emf, stats, helloId, start);
    }
  }

  static void evictionAndOffheap() {
    try (EntityManagerFactory emf = Database.create("ehcache-small-heap.xml", Map.of(), true)) {
      Statistics stats = Database.statistics(emf);

      step("8. Heap of 2 entries, three languages");
      Long[] ids = saveLanguages(emf);
      System.out.println("Language puts=" + stats.getDomainDataRegionStatistics(LANGUAGE).getPutCount()
          + " entries=" + Database.entryCount(emf, LANGUAGE));
      System.out.println("translations puts=" + stats.getDomainDataRegionStatistics("translations").getPutCount()
          + " entries=" + Database.entryCount(emf, "translations"));
      printContains(emf, ids);
      Long evicted = null;
      for (int i = 0; i < 3; i++) {
        if (!emf.getCache().contains(Language.class, ids[i])) {
          evicted = ids[i];
        }
      }
      Long id = evicted;
      stats.clear();
      emf.runInTransaction(em -> System.out.println(em.find(Language.class, id)));
      printRegion(stats, LANGUAGE);
      printContains(emf, ids);

      step("9. Heap of 2 entries plus 10 MB offheap, three translations");
      System.out.println("translations entries=" + Database.entryCount(emf, "translations")
          + " (" + Database.resources(emf, "translations") + ")");
      stats.clear();
      emf.runInTransaction(em -> {
        for (int i = 3; i < 6; i++) {
          System.out.println(em.find(Translation.class, ids[i]));
        }
      });
      printRegion(stats, "translations");
    }
  }

  static void sizeBasedHeap() {
    step("10. Heap of 64 kB instead of entries");
    try (EntityManagerFactory emf = Database.create("ehcache-size.xml", Map.of(), false)) {
      emf.runInTransaction(em -> {
        for (int i = 1; i <= 1000; i++) {
          em.persist(new Language("l" + i, "Language " + i));
        }
      });
      System.out.println("Language entries in 64 kB: " + Database.entryCount(emf, LANGUAGE) + " of 1000");
    }
  }

  static void missingCacheStrategy() {
    for (String strategy : List.of("create-warn", "create", "fail")) {
      step("11. ehcache-missing.xml with missing_cache_strategy=" + strategy);
      try (EntityManagerFactory emf = Database.create("ehcache-missing.xml",
          Map.of("hibernate.javax.cache.missing_cache_strategy", strategy), false)) {
        System.out.println("started; translations -> " + Database.resources(emf, "translations"));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
        System.out.println("Caused by: " + rootCause(e));
      }
    }
  }

  static void concurrencyStrategies() {
    try (EntityManagerFactory emf = Database.create("ehcache-labels.xml", Map.of(), true,
        ReadOnlyLabel.class, NonstrictLabel.class, ReadWriteLabel.class, TransactionalLabel.class)) {
      Statistics stats = Database.statistics(emf);
      tryStrategy(emf, stats, ReadOnlyLabel.class, ReadOnlyLabel::new);
      tryStrategy(emf, stats, NonstrictLabel.class, NonstrictLabel::new);
      tryStrategy(emf, stats, ReadWriteLabel.class, ReadWriteLabel::new);
      tryStrategy(emf, stats, TransactionalLabel.class, TransactionalLabel::new);
    }
  }

  static void regionPrefixAndDefaultTemplate() {
    step("13. hibernate.cache.region_prefix=i18n");
    try (EntityManagerFactory emf = Database.create("ehcache.xml",
        Map.of("hibernate.cache.region_prefix", "i18n", "hibernate.javax.cache.missing_cache_strategy", "create"), false)) {
      System.out.println("caches: " + Database.cacheNames(emf));
      System.out.println("statistics regions: " + List.of(Database.statistics(emf).getSecondLevelCacheRegionNames()));
    }

    step("14. jsr107 default-template for regions without a cache element");
    try (EntityManagerFactory emf = Database.create("ehcache-default-template.xml", Map.of(), false)) {
      for (String name : Database.cacheNames(emf)) {
        System.out.println(name + " -> " + Database.resources(emf, name) + ", " + Database.expiry(emf, name));
      }
    }
  }

  // ---------------------------------------------------------------- helpers

  static Long[] saveLanguages(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Language en = new Language("en", "English");
      Language fr = new Language("fr", "French");
      Language de = new Language("de", "German");
      em.persist(en);
      em.persist(fr);
      em.persist(de);
      Translation hello = en.translate("greeting", "Hello");
      Translation bonjour = fr.translate("greeting", "Bonjour");
      Translation hallo = de.translate("greeting", "Hallo");
      em.persist(hello);
      em.persist(bonjour);
      em.persist(hallo);
      return new Long[] {en.getId(), fr.getId(), de.getId(), hello.getId(), bonjour.getId(), hallo.getId()};
    });
  }

  static List<Language> languagesByCode(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> em
        .createQuery("select l from Language l order by l.code", Language.class)
        .setHint(HibernateHints.HINT_CACHEABLE, true)
        .setHint(HibernateHints.HINT_CACHE_REGION, "language-queries")
        .getResultList());
  }

  static void findLanguage(EntityManagerFactory emf, Statistics stats, Long id, long start) {
    emf.runInTransaction(em -> em.find(Language.class, id));
    CacheRegionStatistics region = stats.getDomainDataRegionStatistics(LANGUAGE);
    System.out.println("t=" + millis(start) + "ms find(en): hits=" + region.getHitCount() + " misses=" + region.getMissCount());
  }

  static void findTranslation(EntityManagerFactory emf, Statistics stats, Long id, long start) {
    emf.runInTransaction(em -> em.find(Translation.class, id));
    CacheRegionStatistics region = stats.getDomainDataRegionStatistics("translations");
    System.out.println("t=" + millis(start) + "ms find(greeting): hits=" + region.getHitCount() + " misses=" + region.getMissCount());
  }

  static <T extends Label> void tryStrategy(EntityManagerFactory emf, Statistics stats, Class<T> type,
      Function<String, T> factory) {
    step("12. " + type.getSimpleName());
    stats.clear();
    Long id = emf.callInTransaction(em -> {
      T label = factory.apply("Save");
      em.persist(label);
      return label.getId();
    });
    System.out.println("insert: puts=" + stats.getDomainDataRegionStatistics(type.getName()).getPutCount()
        + ", in cache=" + emf.getCache().contains(type, id));
    stats.clear();
    try {
      emf.runInTransaction(em -> em.find(type, id).setText("Save changes"));
      System.out.println("update: committed, puts=" + stats.getDomainDataRegionStatistics(type.getName()).getPutCount()
          + ", in cache=" + emf.getCache().contains(type, id));
    } catch (RuntimeException e) {
      System.out.println("update: " + e.getClass().getName() + ": " + e.getMessage());
    }
    stats.clear();
    String text = emf.callInTransaction(em -> em.find(type, id).getText());
    CacheRegionStatistics region = stats.getDomainDataRegionStatistics(type.getName());
    System.out.println("next find: " + text + ", hits=" + region.getHitCount() + ", misses=" + region.getMissCount());
  }

  static void printContains(EntityManagerFactory emf, Long[] ids) {
    System.out.println("in cache: en=" + emf.getCache().contains(Language.class, ids[0])
        + " fr=" + emf.getCache().contains(Language.class, ids[1])
        + " de=" + emf.getCache().contains(Language.class, ids[2]));
  }

  static void printRegion(Statistics stats, String region) {
    CacheRegionStatistics s = stats.getDomainDataRegionStatistics(region);
    System.out.println(region + ": hits=" + s.getHitCount() + " misses=" + s.getMissCount() + " puts=" + s.getPutCount());
  }

  static String rootCause(Throwable e) {
    Throwable cause = e;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause.getClass().getName() + ": " + cause.getMessage();
  }

  static long millis(long start) {
    return (System.nanoTime() - start) / 1_000_000;
  }

  static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
