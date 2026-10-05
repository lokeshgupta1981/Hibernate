package com.howtodoinjava.hibernate.reference;

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

  public static EntityManagerFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("support-desk")
        .managedClasses(Agent.class, Ticket.class)
        .jdbcUrl("jdbc:h2:mem:supportdesk;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.generate_statistics", "true")
        .createEntityManagerFactory();
  }

  /** Resets the statement counter, so a test can count the SQL of one step. */
  public static Statistics statistics(EntityManagerFactory emf) {
    Statistics statistics = emf.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    return statistics;
  }
}
