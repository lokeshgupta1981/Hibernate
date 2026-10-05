package com.howtodoinjava.hibernate.namedprocedure;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;
import org.testcontainers.mysql.MySQLContainer;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  // Starts MySQL in Docker. Any MySQL 8 or newer works the same way.
  public static MySQLContainer startMySql() {
    MySQLContainer mysql = new MySQLContainer("mysql:9");
    mysql.start();
    return mysql;
  }

  public static EntityManagerFactory create(MySQLContainer mysql, boolean showSql) {
    EntityManagerFactory emf = new HibernatePersistenceConfiguration("named-stored-procedure")
        .managedClass(StockItem.class)
        .jdbcUrl(mysql.getJdbcUrl())
        .jdbcCredentials(mysql.getUsername(), mysql.getPassword())
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
    createProcedures(mysql);
    return emf;
  }

  // Each CREATE PROCEDURE in procedures.sql ends with a line containing only "//"
  private static void createProcedures(MySQLContainer mysql) {
    try (InputStream in = Database.class.getResourceAsStream("/procedures.sql");
         Connection con = DriverManager.getConnection(
             mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
         Statement st = con.createStatement()) {
      String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      for (String procedure : sql.split("(?m)^//\\s*$")) {
        if (!procedure.isBlank()) {
          st.execute(procedure.trim());
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }
}
