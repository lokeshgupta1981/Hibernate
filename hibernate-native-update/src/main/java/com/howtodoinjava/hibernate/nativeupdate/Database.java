package com.howtodoinjava.hibernate.nativeupdate;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
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

  public static EntityManagerFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("native-update")
        .managedClasses(Plan.class, Addon.class)
        .jdbcUrl("jdbc:h2:mem:streaming;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        // second-level cache and query cache: JCache with Caffeine
        .property("hibernate.cache.region.factory_class", "jcache")
        .property("hibernate.javax.cache.provider",
            "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider")
        .property("hibernate.javax.cache.missing_cache_strategy", "create")
        .property("hibernate.cache.use_query_cache", true)
        .property("hibernate.generate_statistics", true)
        .createEntityManagerFactory();
  }

  /** Saves four plans and one add-on, returns the ids by name. */
  public static Map<String, Long> seed(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Map<String, Long> ids = new LinkedHashMap<>();
      for (Plan plan : new Plan[] {
          new Plan("Basic", "7.99", true),
          new Plan("Standard", "12.99", true),
          new Plan("Premium", "18.99", true),
          new Plan("Legacy HD", "9.99", false)}) {
        em.persist(plan);
        ids.put(plan.getName(), plan.getId());
      }
      Addon sports = new Addon("Sports", "5.00");
      em.persist(sports);
      ids.put("Sports", sports.getId());
      return ids;
    });
  }

  /** Empties both tables and the second-level cache, then saves the starting data again. */
  public static Map<String, Long> reset(EntityManagerFactory emf) {
    emf.getSchemaManager().truncate();
    emf.getCache().evictAll();
    return seed(emf);
  }

  /** Reads the price straight from the table, bypassing every cache. */
  public static BigDecimal priceInTable(EntityManagerFactory emf, String name) {
    return emf.callInTransaction(em -> (BigDecimal) em
        .createNativeQuery("select monthly_price from plan where name = :name", BigDecimal.class)
        .setParameter("name", name)
        .getSingleResult());
  }

  public static Statistics statistics(EntityManagerFactory emf) {
    return emf.unwrap(SessionFactory.class).getStatistics();
  }
}
