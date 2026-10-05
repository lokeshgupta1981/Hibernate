package com.howtodoinjava.hibernate.testing;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  public static final String H2_URL =
      "jdbc:h2:mem:invoices;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /** Hibernate settings for an in-memory H2 database; import.sql runs after the schema is created. */
  public static HibernatePersistenceConfiguration h2(String url) {
    return new HibernatePersistenceConfiguration("invoices-test")
        .managedClasses(Invoice.class)
        .jdbcUrl(url)
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP);
  }

  public static EntityManagerFactory inMemory(boolean showSql) {
    return h2(H2_URL)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }
}
