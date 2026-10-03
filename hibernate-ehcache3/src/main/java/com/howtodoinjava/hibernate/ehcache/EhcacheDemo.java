package com.howtodoinjava.hibernate.ehcache;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.stat.Statistics;

public class EhcacheDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {
      Statistics stats = Database.statistics(emf);

      step("1. Save three airports and an airline with two hubs");
      Long airlineId = Database.saveSampleData(emf);
      counters(stats);

      var airportRegion = stats.getDomainDataRegionStatistics(Airport.class.getName());
      System.out.println("Airport region: puts=" + airportRegion.getPutCount());

      step("2. Empty the cache and the counters");
      emf.getCache().evictAll();
      stats.clear();
      System.out.println("contains DEL: " + emf.getCache().contains(Airport.class, "DEL"));

      step("3. Session 1: find DEL");
      emf.runInTransaction(em -> System.out.println(em.find(Airport.class, "DEL")));
      counters(stats);

      step("4. Session 2: find DEL again");
      emf.runInTransaction(em -> System.out.println(em.find(Airport.class, "DEL")));
      counters(stats);

      step("5. Session 3 and 4: read the hubs of the airline");
      stats.clear();
      emf.runInTransaction(em -> System.out.println(em.find(Airline.class, airlineId).hubCities()));
      counters(stats);
      emf.runInTransaction(em -> System.out.println(em.find(Airline.class, airlineId).hubCities()));
      counters(stats);

      step("6. Cached query in two sessions");
      stats.clear();
      for (int i = 1; i <= 2; i++) {
        emf.runInTransaction(em -> System.out.println(airportsIn(em, "Mumbai")));
        System.out.println("query cache hits=" + stats.getQueryCacheHitCount()
            + ", misses=" + stats.getQueryCacheMissCount()
            + ", puts=" + stats.getQueryCachePutCount()
            + ", statements=" + stats.getPrepareStatementCount());
      }

      step("7. Regions created in Ehcache");
      for (String region : stats.getSecondLevelCacheRegionNames()) {
        System.out.println(region);
      }
    }

    step("8. Startup errors from a wrong or missing setting");
    tryStart("hibernate.javax.cache.uri=ehcache-missing.xml",
        Map.of("hibernate.javax.cache.uri", "ehcache-missing.xml"));
    tryStart("hibernate.javax.cache.provider=org.ehcache.EhcacheProvider",
        Map.of("hibernate.javax.cache.provider", "org.ehcache.EhcacheProvider"));
    tryStart("ehcache-no-hubs.xml with missing_cache_strategy=fail",
        Map.of("hibernate.javax.cache.uri", "ehcache-no-hubs.xml"));
    tryStart("ehcache-no-hubs.xml with missing_cache_strategy=create-warn",
        Map.of("hibernate.javax.cache.uri", "ehcache-no-hubs.xml",
            "hibernate.javax.cache.missing_cache_strategy", "create-warn"));
    tryStart("region.factory_class=ehcache",
        Map.of("hibernate.cache.region.factory_class", "ehcache"));
    Map<String, Object> noUri = new HashMap<>();
    noUri.put("hibernate.javax.cache.uri", null);
    tryStart("no hibernate.javax.cache.uri", noUri);
    Map<String, Object> wrongKey = new HashMap<>(noUri);
    wrongKey.put("hibernate.cache.jcache.uri", "ehcache.xml");
    tryStart("hibernate.cache.jcache.uri instead of hibernate.javax.cache.uri", wrongKey);
  }

  static List<Airport> airportsIn(jakarta.persistence.EntityManager em, String city) {
    return em.createQuery("select a from Airport a where a.city = :city", Airport.class)
        .setParameter("city", city)
        .setHint(HibernateHints.HINT_CACHEABLE, true)
        .getResultList();
  }

  static void tryStart(String label, Map<String, Object> overrides) {
    System.out.println("-- " + label);
    try (EntityManagerFactory emf = Database.create(false, overrides)) {
      System.out.println("started");
    } catch (RuntimeException e) {
      Throwable t = e;
      while (t != null) {
        System.out.println("   " + t.getClass().getName() + ": " + t.getMessage());
        t = t.getCause();
      }
    }
  }

  static void counters(Statistics stats) {
    System.out.println("L2 hits=" + stats.getSecondLevelCacheHitCount()
        + ", misses=" + stats.getSecondLevelCacheMissCount()
        + ", puts=" + stats.getSecondLevelCachePutCount()
        + ", statements=" + stats.getPrepareStatementCount());
  }

  static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }
}
