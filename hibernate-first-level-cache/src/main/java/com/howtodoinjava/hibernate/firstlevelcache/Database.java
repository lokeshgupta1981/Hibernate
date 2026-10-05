package com.howtodoinjava.hibernate.firstlevelcache;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return create(showSql, "movies");
  }

  public static EntityManagerFactory create(boolean showSql, String databaseName) {
    return new HibernatePersistenceConfiguration("first-level-cache")
        .managedClasses(Movie.class)
        .jdbcUrl("jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property(AvailableSettings.STATEMENT_INSPECTOR, new SqlCounter())
        .property(AvailableSettings.GENERATE_STATISTICS, true)
        .property(AvailableSettings.STATEMENT_BATCH_SIZE, 50)
        .createEntityManagerFactory();
  }

  /** Saves three movies and returns their ids. */
  public static List<Long> saveMovies(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Movie inception = new Movie("Inception", 2010, 8.8);
      Movie up = new Movie("Up", 2009, 8.3);
      Movie coco = new Movie("Coco", 2017, 8.4);
      em.persist(inception);
      em.persist(up);
      em.persist(coco);
      return List.of(inception.getId(), up.getId(), coco.getId());
    });
  }
}
