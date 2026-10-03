package com.howtodoinjava.hibernate.ehcacheconfig;

import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.locks.LockSupport;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.ResourceType;
import org.ehcache.config.SizedResourcePool;
import org.ehcache.jsr107.Eh107Configuration;
import org.hibernate.SessionFactory;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.stat.Statistics;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /** Main configuration: ehcache.xml from the classpath. */
  public static EntityManagerFactory create(boolean showSql) {
    return create("ehcache.xml", Map.of(), showSql);
  }

  /** ehcacheXml: a classpath resource such as "ehcache.xml", or null for the default CacheManager. */
  public static EntityManagerFactory create(String ehcacheXml, Map<String, Object> extraProperties,
      boolean showSql, Class<?>... extraEntities) {
    HibernatePersistenceConfiguration config = new HibernatePersistenceConfiguration("ehcache-config")
        .managedClasses(Language.class, Translation.class)
        .jdbcUrl("jdbc:h2:mem:i18n;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.cache.region.factory_class", "jcache")
        .property("hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider")
        .property("hibernate.cache.use_query_cache", true)
        .property("hibernate.generate_statistics", true);
    if (ehcacheXml != null) {
      config.property("hibernate.javax.cache.uri", ehcacheXml);
    }
    config.managedClasses(List.of(extraEntities));
    extraProperties.forEach(config::property);
    return config.createEntityManagerFactory();
  }

  public static Statistics statistics(EntityManagerFactory emf) {
    return emf.unwrap(SessionFactory.class).getStatistics();
  }

  /** The JCache CacheManager that Hibernate created from ehcache.xml. */
  public static CacheManager cacheManager(EntityManagerFactory emf) {
    JCacheRegionFactory regionFactory = (JCacheRegionFactory) emf.unwrap(SessionFactoryImplementor.class)
        .getCache().getRegionFactory();
    return regionFactory.getCacheManager();
  }

  public static List<String> cacheNames(EntityManagerFactory emf) {
    List<String> names = new ArrayList<>();
    cacheManager(emf).getCacheNames().forEach(names::add);
    return new ArrayList<>(new TreeSet<>(names));
  }

  /** Number of entries currently stored in one Ehcache cache. */
  public static int entryCount(EntityManagerFactory emf, String alias) {
    int count = 0;
    for (Cache.Entry<Object, Object> ignored : cacheManager(emf).getCache(alias)) {
      count++;
    }
    return count;
  }

  /** The resource pools (heap, offheap) Ehcache uses for one cache, for example "heap=500 entries". */
  @SuppressWarnings("unchecked")
  public static String resources(EntityManagerFactory emf, String alias) {
    Cache<Object, Object> cache = cacheManager(emf).getCache(alias);
    ResourcePools pools = runtimeConfiguration(cache).getResourcePools();
    List<String> parts = new ArrayList<>();
    for (ResourceType<?> type : pools.getResourceTypeSet()) {
      SizedResourcePool pool = pools.getPoolForResource((ResourceType<SizedResourcePool>) type);
      parts.add(type.toString().toLowerCase() + "=" + pool.getSize() + " " + pool.getUnit().toString().toLowerCase());
    }
    parts.sort(null);
    return String.join(", ", parts);
  }

  /** The expiry policy Ehcache uses for one cache. */
  public static String expiry(EntityManagerFactory emf, String alias) {
    Cache<Object, Object> cache = cacheManager(emf).getCache(alias);
    return String.valueOf(runtimeConfiguration(cache).getExpiryPolicy());
  }

  @SuppressWarnings("unchecked")
  private static CacheRuntimeConfiguration<Object, Object> runtimeConfiguration(Cache<Object, Object> cache) {
    Eh107Configuration<Object, Object> configuration = cache.getConfiguration(Eh107Configuration.class);
    return configuration.unwrap(CacheRuntimeConfiguration.class);
  }

  /** Waits without blocking a lock; used by the expiry examples. */
  public static void pause(Duration duration) {
    long deadline = System.nanoTime() + duration.toNanos();
    long left;
    while ((left = deadline - System.nanoTime()) > 0) {
      LockSupport.parkNanos(left);
    }
  }
}
