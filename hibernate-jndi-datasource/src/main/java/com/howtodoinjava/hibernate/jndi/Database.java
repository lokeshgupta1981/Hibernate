package com.howtodoinjava.hibernate.jndi;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  /** The name the application server uses, for example a Tomcat Resource named jdbc/cities. */
  public static final String JNDI_NAME = "java:comp/env/jdbc/cities";

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /** Production style: Hibernate looks the DataSource up by its JNDI name. */
  public static EntityManagerFactory fromJndi(String jndiName, boolean showSql) {
    return new HibernatePersistenceConfiguration("cities")
        .managedClasses(City.class)
        .nonJtaDataSource(jndiName)
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  /** No JNDI: Hibernate gets the DataSource object itself. */
  public static EntityManagerFactory fromDataSource(DataSource dataSource, boolean showSql) {
    return new HibernatePersistenceConfiguration("cities")
        .managedClasses(City.class)
        .property(JdbcSettings.JAKARTA_NON_JTA_DATASOURCE, dataSource)
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  /** An H2 in-memory DataSource; DB_CLOSE_DELAY=-1 keeps the database while the JVM runs. */
  public static DataSource h2(String databaseName) {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1");
    dataSource.setUser("sa");
    dataSource.setPassword("");
    return dataSource;
  }
}
