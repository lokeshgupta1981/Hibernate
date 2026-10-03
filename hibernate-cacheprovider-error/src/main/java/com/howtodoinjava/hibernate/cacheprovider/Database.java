package com.howtodoinjava.hibernate.cacheprovider;

import jakarta.persistence.EntityManagerFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.stat.Statistics;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /** Hibernate 7 second level cache settings: JCache with Ehcache 3. */
  public static Map<String, Object> jcacheSettings() {
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put("hibernate.cache.region.factory_class", "jcache");
    settings.put("hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider");
    settings.put("hibernate.javax.cache.uri", "ehcache.xml");
    settings.put("hibernate.javax.cache.missing_cache_strategy", "fail");
    return settings;
  }

  /** Hibernate 3 style settings: only hibernate.cache.provider_class, no region factory. */
  public static Map<String, Object> hibernate3Settings() {
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put("hibernate.cache.use_second_level_cache", true);
    settings.put("hibernate.cache.provider_class", "org.hibernate.cache.EhCacheProvider");
    return settings;
  }

  public static EntityManagerFactory create(boolean showSql) {
    return create(showSql, jcacheSettings());
  }

  public static EntityManagerFactory create(boolean showSql, Map<String, Object> cacheSettings) {
    return new HibernatePersistenceConfiguration("postal-codes")
        .managedClasses(PostalCode.class)
        .jdbcUrl("jdbc:h2:mem:postal;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.generate_statistics", true)
        .properties(cacheSettings)
        .createEntityManagerFactory();
  }

  public static Statistics statistics(EntityManagerFactory emf) {
    return emf.unwrap(SessionFactory.class).getStatistics();
  }

  public static void saveSampleData(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      em.persist(new PostalCode("560001", "Bengaluru", "Bengaluru Urban"));
      em.persist(new PostalCode("411001", "Pune", "Pune"));
      em.persist(new PostalCode("302001", "Jaipur", "Jaipur"));
    });
  }
}
