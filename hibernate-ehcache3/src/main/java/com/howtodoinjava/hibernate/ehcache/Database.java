package com.howtodoinjava.hibernate.ehcache;

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

  /** The second level cache settings used in the article. */
  public static Map<String, Object> cacheSettings() {
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put("hibernate.cache.use_second_level_cache", true);
    settings.put("hibernate.cache.region.factory_class", "jcache");
    settings.put("hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider");
    settings.put("hibernate.javax.cache.uri", "ehcache.xml");
    settings.put("hibernate.javax.cache.missing_cache_strategy", "fail");
    settings.put("hibernate.cache.use_query_cache", true);
    settings.put("hibernate.generate_statistics", true);
    return settings;
  }

  public static EntityManagerFactory create(boolean showSql) {
    return create(showSql, Map.of());
  }

  /**
   * Creates the factory with the article's cache settings. An entry in {@code overrides}
   * replaces a setting; a {@code null} value removes it.
   */
  public static EntityManagerFactory create(boolean showSql, Map<String, Object> overrides) {
    Map<String, Object> settings = cacheSettings();
    overrides.forEach((key, value) -> {
      if (value == null) {
        settings.remove(key);
      } else {
        settings.put(key, value);
      }
    });
    return new HibernatePersistenceConfiguration("ehcache3")
        .managedClasses(Airport.class, Airline.class)
        .jdbcUrl("jdbc:h2:mem:airports;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .properties(settings)
        .createEntityManagerFactory();
  }

  public static Statistics statistics(EntityManagerFactory emf) {
    return emf.unwrap(SessionFactory.class).getStatistics();
  }

  /** Saves three airports and one airline with two hubs, and returns the airline id. */
  public static Long saveSampleData(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Airport delhi = new Airport("DEL", "Indira Gandhi", "Delhi");
      Airport mumbai = new Airport("BOM", "Chhatrapati Shivaji", "Mumbai");
      Airport bengaluru = new Airport("BLR", "Kempegowda", "Bengaluru");
      em.persist(delhi);
      em.persist(mumbai);
      em.persist(bengaluru);

      Airline airIndia = new Airline("Air India");
      airIndia.addHub(delhi);
      airIndia.addHub(mumbai);
      em.persist(airIndia);
      return airIndia.getId();
    });
  }
}
