package com.howtodoinjava.hibernate.getload;

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

  /** HibernatePersistenceConfiguration returns a SessionFactory, which is also an EntityManagerFactory. */
  public static SessionFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("parcel-tracking")
        .managedClasses(Parcel.class, ScanEvent.class)
        .jdbcUrl("jdbc:h2:mem:parcels;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.generate_statistics", "true")
        .property("hibernate.session_factory.statement_inspector", SqlLog.class.getName())
        .createEntityManagerFactory();
  }

  /** Resets the statement counter, so a test can count the SQL of one step. */
  public static Statistics statistics(SessionFactory sf) {
    Statistics statistics = sf.getStatistics();
    statistics.clear();
    return statistics;
  }
}
