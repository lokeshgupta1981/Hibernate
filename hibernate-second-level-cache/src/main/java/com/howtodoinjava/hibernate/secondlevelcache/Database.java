package com.howtodoinjava.hibernate.secondlevelcache;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.stat.Statistics;
import org.hibernate.tool.schema.Action;

public final class Database {

  public static final String URL = "jdbc:h2:mem:atlas;DB_CLOSE_DELAY=-1";

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql, Class<?>... extraEntities) {
    HibernatePersistenceConfiguration config = new HibernatePersistenceConfiguration("second-level-cache")
        .managedClasses(Country.class, Currency.class)
        .jdbcUrl(URL)
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        // second level cache: JCache with Ehcache 3
        .property("hibernate.cache.region.factory_class", "jcache")
        .property("hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider")
        .property("hibernate.javax.cache.uri", "ehcache.xml")
        .property("hibernate.cache.use_query_cache", true)
        .property("hibernate.generate_statistics", true);
    config.managedClasses(List.of(extraEntities));
    return config.createEntityManagerFactory();
  }

  public static Statistics statistics(EntityManagerFactory emf) {
    return emf.unwrap(SessionFactory.class).getStatistics();
  }
}
