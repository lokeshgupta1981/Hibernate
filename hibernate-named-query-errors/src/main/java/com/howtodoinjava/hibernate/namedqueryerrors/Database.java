package com.howtodoinjava.hibernate.namedqueryerrors;

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

  /** Volunteer, Shift and any extra classes that carry (broken) named queries. */
  public static HibernatePersistenceConfiguration configuration(boolean showSql, Class<?>... extra) {
    return base(showSql)
        .managedClasses(Volunteer.class, Shift.class)
        .managedClasses(extra);
  }

  /** No managed classes yet: the caller registers them. */
  public static HibernatePersistenceConfiguration base(boolean showSql) {
    return new HibernatePersistenceConfiguration("volunteer-shifts")
        .jdbcUrl("jdbc:h2:mem:shifts;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false);
  }

  public static EntityManagerFactory create(boolean showSql, Class<?>... extra) {
    return configuration(showSql, extra).createEntityManagerFactory();
  }

  public static void seed(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      Volunteer lokesh = new Volunteer("Lokesh", "555-0101");
      Volunteer anna = new Volunteer("Anna", "555-0102");
      em.persist(lokesh);
      em.persist(anna);
      em.persist(new Shift("Food Drive", LocalDateTime.of(2026, 10, 10, 9, 0), 4, lokesh));
      em.persist(new Shift("Park Cleanup", LocalDateTime.of(2026, 10, 11, 8, 0), 3, anna));
      em.persist(new Shift("Book Fair", LocalDateTime.of(2026, 10, 12, 10, 0), 6, lokesh));
    });
  }
}
