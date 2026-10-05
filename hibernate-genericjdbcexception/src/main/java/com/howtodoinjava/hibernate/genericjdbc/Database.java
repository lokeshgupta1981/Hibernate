package com.howtodoinjava.hibernate.genericjdbc;

import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

/**
 * Builds Hibernate configurations for the three databases used in the examples.
 * Callers add properties and then call createEntityManagerFactory().
 */
public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /** H2 in-memory database; the dialect is detected from the connection. */
  public static HibernatePersistenceConfiguration h2(String name, Action action, boolean showSql,
      Class<?>... entities) {
    return new HibernatePersistenceConfiguration(name)
        .managedClasses(entities)
        .jdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(action)
        .showSql(showSql, false, false);
  }

  /** SQLite in-memory database with the dialect from hibernate-community-dialects. */
  public static HibernatePersistenceConfiguration sqlite(String name, Action action, boolean showSql,
      Class<?>... entities) {
    return new HibernatePersistenceConfiguration(name)
        .managedClasses(entities)
        .jdbcUrl("jdbc:sqlite:file:" + name + "?mode=memory&cache=shared")
        .property("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect")
        .schemaToolingAction(action)
        .showSql(showSql, false, false);
  }

  /** HSQLDB in-memory database; the driver version comes from the pom (hsqldb.version). */
  public static HibernatePersistenceConfiguration hsqldb(String name, Action action, boolean showSql,
      Class<?>... entities) {
    return new HibernatePersistenceConfiguration(name)
        .managedClasses(entities)
        .jdbcDriver("org.hsqldb.jdbcDriver")
        .jdbcUrl("jdbc:hsqldb:mem:" + name)
        .jdbcCredentials("sa", "")
        .schemaToolingAction(action)
        .showSql(showSql, false, false);
  }
}
