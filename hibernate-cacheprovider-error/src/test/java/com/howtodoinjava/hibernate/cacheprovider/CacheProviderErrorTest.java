package com.howtodoinjava.hibernate.cacheprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;

class CacheProviderErrorTest {

  private static Throwable rootCause(Throwable e) {
    Throwable t = e;
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t;
  }

  private static Throwable startWith(Map<String, Object> settings) {
    return assertThrows(RuntimeException.class, () -> Database.create(false, settings).close());
  }

  // 1. The error itself

  @Test
  void hibernate7HasNoCacheProviderInterface() {
    ClassNotFoundException e = assertThrows(ClassNotFoundException.class,
        () -> Class.forName("org.hibernate.cache.CacheProvider"));
    assertEquals("org.hibernate.cache.CacheProvider", e.getMessage());
  }

  @Test
  void hibernate7HasRegionFactory() throws ClassNotFoundException {
    assertNotNull(Class.forName("org.hibernate.cache.spi.RegionFactory"));
  }

  @Test
  void oldEhCacheProviderCannotBeLoaded() {
    // hibernate-ehcache 3.6.10: class EhCacheProvider implements CacheProvider
    NoClassDefFoundError e = assertThrows(NoClassDefFoundError.class,
        () -> Class.forName("org.hibernate.cache.EhCacheProvider"));
    assertEquals("org/hibernate/cache/CacheProvider", e.getMessage());
    assertInstanceOf(ClassNotFoundException.class, e.getCause());
  }

  @Test
  void springHibernate3FactoryBeanFailsOnItsFields() throws ClassNotFoundException {
    // spring-orm 3.2: the class loads, reading its fields fails on "private CacheProvider cacheProvider"
    Class<?> factoryBean = Class.forName("org.springframework.orm.hibernate3.LocalSessionFactoryBean");
    NoClassDefFoundError e = assertThrows(NoClassDefFoundError.class, factoryBean::getDeclaredFields);
    assertEquals("org/hibernate/cache/CacheProvider", e.getMessage());
  }

  // 2. Old settings on Hibernate 7

  @Test
  void oldProviderAsRegionFactoryFailsAtStartup() {
    Throwable e = startWith(Map.of("hibernate.cache.region.factory_class", "org.hibernate.cache.EhCacheProvider"));
    assertTrue(e.getMessage().contains(
        "Unable to resolve name [org.hibernate.cache.EhCacheProvider] as strategy [org.hibernate.cache.spi.RegionFactory]"),
        e.getMessage());
    // Hibernate tries several class loaders and keeps each NoClassDefFoundError as a suppressed exception
    Throwable root = rootCause(e);
    assertInstanceOf(ClassNotFoundException.class, root);
    assertEquals("Could not load requested class: org.hibernate.cache.EhCacheProvider", root.getMessage());
    assertTrue(root.getSuppressed().length > 0);
    for (Throwable suppressed : root.getSuppressed()) {
      assertInstanceOf(NoClassDefFoundError.class, suppressed);
      assertEquals("org/hibernate/cache/CacheProvider", suppressed.getMessage());
    }
  }

  @Test
  void hibernate4RegionFactoryNameIsUnknown() {
    Throwable e = startWith(Map.of("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory"));
    Throwable root = rootCause(e);
    assertInstanceOf(ClassNotFoundException.class, root);
    assertEquals("Could not load requested class: org.hibernate.cache.ehcache.EhCacheRegionFactory", root.getMessage());
  }

  @Test
  void hibernate5ShortNameIsUnknown() {
    Throwable e = startWith(Map.of("hibernate.cache.region.factory_class", "ehcache"));
    assertEquals("Could not load requested class: ehcache", rootCause(e).getMessage());
  }

  @Test
  void providerClassPropertyIsIgnored() {
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put("hibernate.cache.use_second_level_cache", true);
    settings.put("hibernate.cache.provider_class", "org.example.NoSuchProvider");
    try (EntityManagerFactory emf = Database.create(false, settings)) {
      // starts without error: Hibernate 7 never reads the key
      // the cache still works only because hibernate-jcache is the single region factory on the classpath
      assertInstanceOf(JCacheRegionFactory.class,
          emf.unwrap(SessionFactoryImplementor.class).getCache().getRegionFactory());
    }
  }

  // 3. The Hibernate 7 setup

  @Test
  void jcacheSetupServesSecondLookupFromCache() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Database.saveSampleData(emf);
      Statistics stats = Database.statistics(emf);
      emf.getCache().evictAll();
      stats.clear();

      PostalCode first = emf.callInTransaction(em -> em.find(PostalCode.class, "560001"));
      assertEquals("Bengaluru", first.getTown());
      assertEquals(0, stats.getSecondLevelCacheHitCount());
      assertEquals(1, stats.getSecondLevelCacheMissCount());
      assertEquals(1, stats.getSecondLevelCachePutCount());
      assertEquals(1, stats.getPrepareStatementCount());

      PostalCode second = emf.callInTransaction(em -> em.find(PostalCode.class, "560001"));
      assertEquals("Bengaluru Urban", second.getDistrict());
      assertEquals(1, stats.getSecondLevelCacheHitCount());
      assertEquals(1, stats.getPrepareStatementCount());
      assertTrue(emf.getCache().contains(PostalCode.class, "560001"));
    }
  }

  @Test
  void jcacheSetupUsesJCacheRegionFactory() {
    try (EntityManagerFactory emf = Database.create(false)) {
      assertInstanceOf(JCacheRegionFactory.class,
          emf.unwrap(SessionFactoryImplementor.class).getCache().getRegionFactory());
    }
  }

  @Test
  void persistFillsTheCache() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Statistics stats = Database.statistics(emf);
      stats.clear();
      Database.saveSampleData(emf);
      assertEquals(3, stats.getSecondLevelCachePutCount());
    }
  }
}
