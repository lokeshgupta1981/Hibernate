package com.howtodoinjava.hibernate.notfound;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
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
   * Creates an EntityManagerFactory with Scholarship and one variant of the application mapping,
   * each in its own in-memory database.
   */
  public static EntityManagerFactory create(Class<? extends BaseApplication> applicationClass,
      boolean showSql) {
    String name = applicationClass.getSimpleName();
    return new HibernatePersistenceConfiguration(name)
        .managedClasses(Scholarship.class, applicationClass)
        .jdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.generate_statistics", true)
        .createEntityManagerFactory();
  }

  /** Saves the scholarship "Merit" and two applications with plain SQL, as a legacy import would. */
  public static Long insertLegacyData(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Scholarship merit = new Scholarship("Merit", new BigDecimal("500.00"));
      em.persist(merit);
      em.createNativeQuery("""
          insert into scholarship_application (studentName, submittedOn, scholarship_id)
          values ('Lokesh', date '2026-09-15', 99)""").executeUpdate();
      em.createNativeQuery("""
          insert into scholarship_application (studentName, submittedOn, scholarship_id)
          values ('Alex', date '2026-09-16', ?)""").setParameter(1, merit.getId()).executeUpdate();
      return merit.getId();
    });
  }

  /** Number of JDBC statements prepared since the last call. */
  public static long statements(EntityManagerFactory emf) {
    Statistics statistics = emf.unwrap(SessionFactory.class).getStatistics();
    long count = statistics.getPrepareStatementCount();
    statistics.clear();
    return count;
  }
}
