package com.howtodoinjava.hibernate.parameters;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalTime;
import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /** Default Hibernate settings. */
  public static EntityManagerFactory create(boolean showSql) {
    return create(showSql, false);
  }

  /** strictJpa = true turns on hibernate.jpa.compliance.query. */
  public static EntityManagerFactory create(boolean showSql, boolean strictJpa) {
    EntityManagerFactory emf = new HibernatePersistenceConfiguration("train-timetable")
        .managedClasses(TrainService.class)
        .jdbcUrl("jdbc:h2:mem:timetable" + (strictJpa ? "-strict" : "") + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .property(JpaComplianceSettings.JPA_QUERY_COMPLIANCE, strictJpa)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();

    emf.runInTransaction(em -> {
      em.persist(new TrainService(101, "Delhi", "Mumbai", LocalTime.of(6, 0)));
      em.persist(new TrainService(102, "Delhi", "Mumbai", LocalTime.of(16, 30)));
      em.persist(new TrainService(201, "Delhi", "Jaipur", LocalTime.of(7, 15)));
      em.persist(new TrainService(301, "Mumbai", "Pune", LocalTime.of(9, 45)));
      em.createNativeQuery("create alias if not exists count_departures for "
          + "\"com.howtodoinjava.hibernate.parameters.Procedures.countDepartures\"").executeUpdate();
    });
    return emf;
  }
}
