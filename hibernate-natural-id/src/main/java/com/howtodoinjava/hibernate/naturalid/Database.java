package com.howtodoinjava.hibernate.naturalid;

import jakarta.persistence.EntityManagerFactory;
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

  /**
   * @param showSql          print every SQL statement
   * @param secondLevelCache turn the second-level cache (Ehcache through JCache) on or off
   */
  public static EntityManagerFactory create(boolean showSql, boolean secondLevelCache) {
    return create(showSql, secondLevelCache, new SqlLog());
  }

  public static EntityManagerFactory create(boolean showSql, boolean secondLevelCache, SqlLog sqlLog) {
    return new HibernatePersistenceConfiguration("natural-id")
        .managedClasses(Vehicle.class, Driver.class, ParkingSpot.class)
        .jdbcUrl("jdbc:h2:mem:garage" + (secondLevelCache ? "cached" : "plain") + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.generate_statistics", true)
        .property("hibernate.session_factory.statement_inspector", sqlLog)
        .property("hibernate.cache.use_second_level_cache", secondLevelCache)
        .property("hibernate.cache.region.factory_class", "jcache")
        .property("hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider")
        .property("hibernate.javax.cache.missing_cache_strategy", "create")
        .createEntityManagerFactory();
  }

  public static Statistics statistics(EntityManagerFactory emf) {
    return emf.unwrap(SessionFactory.class).getStatistics();
  }

  /** Number of JDBC statements Hibernate prepared since the last reset. */
  public static long statements(EntityManagerFactory emf) {
    return statistics(emf).getPrepareStatementCount();
  }

  public static void resetStatistics(EntityManagerFactory emf) {
    statistics(emf).clear();
  }
}
