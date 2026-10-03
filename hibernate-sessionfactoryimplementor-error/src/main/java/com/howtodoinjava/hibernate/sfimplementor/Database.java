package com.howtodoinjava.hibernate.sfimplementor;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("fixtures")
        .managedClasses(Fixture.class)
        .jdbcUrl("jdbc:h2:mem:league;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  public static void saveSampleData(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      em.persist(new Fixture("Rovers", "United", LocalDateTime.of(2026, 10, 10, 15, 0), "Riverside"));
      em.persist(new Fixture("City", "Athletic", LocalDateTime.of(2026, 10, 11, 17, 30), "Hill Park"));
    });
  }
}
