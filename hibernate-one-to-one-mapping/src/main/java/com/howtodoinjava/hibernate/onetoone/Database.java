package com.howtodoinjava.hibernate.onetoone;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.stat.Statistics;
import org.hibernate.tool.schema.Action;

/**
 * Each mapping variant gets its own in-memory database, so every variant can use the same
 * table names (booking, boarding_pass).
 */
public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(String name, boolean showSql, Class<?>... entities) {
    return new HibernatePersistenceConfiguration(name)
        .managedClasses(entities)
        .jdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.generate_statistics", true)
        .createEntityManagerFactory();
  }

  /** Number of SQL statements Hibernate sent to the database since the last reset. */
  public static long statements(EntityManagerFactory emf) {
    return stats(emf).getPrepareStatementCount();
  }

  public static void resetStatements(EntityManagerFactory emf) {
    stats(emf).clear();
  }

  public static long count(EntityManagerFactory emf, String entity) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from " + entity, Long.class).getSingleResult());
  }

  private static Statistics stats(EntityManagerFactory emf) {
    return emf.unwrap(SessionFactory.class).getStatistics();
  }
}
