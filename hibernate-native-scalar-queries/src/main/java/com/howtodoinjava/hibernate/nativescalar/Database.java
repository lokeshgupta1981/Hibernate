package com.howtodoinjava.hibernate.nativescalar;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("native-scalar")
        .managedClasses(Sighting.class)
        .jdbcUrl("jdbc:h2:mem:birds;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  public static void seed(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      em.persist(new Sighting("Robin", "Lake Park", 3, LocalDate.of(2026, 9, 20)));
      em.persist(new Sighting("Heron", "River Bend", 1, LocalDate.of(2026, 9, 21)));
      em.persist(new Sighting("Robin", "River Bend", 2, LocalDate.of(2026, 9, 22)));
      em.persist(new Sighting("Kingfisher", "River Bend", 1, LocalDate.of(2026, 9, 25)));
    });
  }
}
