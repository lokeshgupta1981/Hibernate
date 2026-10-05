package com.howtodoinjava.hibernate.hsqldbcast;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static String hsqldb(String name) {
    return "jdbc:hsqldb:mem:" + name;
  }

  public static String h2(String name) {
    return "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1";
  }

  public static EntityManagerFactory create(String jdbcUrl, Action action, boolean showSql, Class<?>... entities) {
    return new HibernatePersistenceConfiguration("bus-passes")
        .managedClasses(entities)
        .jdbcUrl(jdbcUrl)
        .jdbcCredentials("sa", "")
        .schemaToolingAction(action)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  /** Runs plain SQL with JDBC, for example to create a table the way an older version of the app did. */
  public static void execute(String jdbcUrl, String... statements) {
    try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
         Statement statement = connection.createStatement()) {
      for (String sql : statements) {
        statement.execute(sql);
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }
}
