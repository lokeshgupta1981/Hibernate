package com.howtodoinjava.hibernate.cacheprovider;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.Statistics;

public class CacheProviderErrorDemo {

  public static void main(String[] args) {
    System.out.println("== 1. Spring orm.hibernate3 LocalSessionFactoryBean on Hibernate 7");
    try {
      Class<?> factoryBean = Class.forName("org.springframework.orm.hibernate3.LocalSessionFactoryBean");
      System.out.println("class loaded: " + factoryBean.getName());
      factoryBean.getDeclaredFields();
    } catch (Throwable e) {
      printCauses(e);
    }

    // Hibernate 4 and 5 region factory names; the Hibernate 3 provider class is tested in CacheProviderErrorTest
    System.out.println("== 2. Hibernate 4 and 5 names in hibernate.cache.region.factory_class");
    for (String name : new String[] {
        "org.hibernate.cache.ehcache.EhCacheRegionFactory",
        "ehcache"}) {
      System.out.println("-- " + name);
      try (EntityManagerFactory emf = Database.create(false, Map.of("hibernate.cache.region.factory_class", name))) {
        System.out.println("started");
      } catch (Throwable e) {
        printCauses(e);
      }
    }

    System.out.println("== 3. Hibernate 7 with JCache and Ehcache 3");
    try (EntityManagerFactory emf = Database.create(true)) {
      Database.saveSampleData(emf);
      Statistics stats = Database.statistics(emf);
      emf.getCache().evictAll();
      stats.clear();

      for (int session = 1; session <= 2; session++) {
        PostalCode code = emf.callInTransaction(em -> em.find(PostalCode.class, "560001"));
        System.out.println("session " + session + ": " + code
            + "  L2 hits=" + stats.getSecondLevelCacheHitCount()
            + ", misses=" + stats.getSecondLevelCacheMissCount()
            + ", puts=" + stats.getSecondLevelCachePutCount()
            + ", statements=" + stats.getPrepareStatementCount());
      }
      System.out.println("region factory: "
          + emf.unwrap(SessionFactoryImplementor.class).getCache().getRegionFactory().getClass().getName());
    }
  }

  private static void printCauses(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      System.out.println("  " + t);
      for (Throwable suppressed : t.getSuppressed()) {
        System.out.println("    suppressed: " + suppressed);
      }
    }
  }
}
