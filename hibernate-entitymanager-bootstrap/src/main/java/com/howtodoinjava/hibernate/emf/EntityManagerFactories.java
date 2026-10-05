package com.howtodoinjava.hibernate.emf;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;
import java.util.Map;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.tool.schema.Action;

/**
 * Every way to create an EntityManagerFactory in Hibernate 7.4 / Jakarta Persistence 3.2. Each
 * method uses its own in-memory database, so a test can check which settings were applied.
 */
public final class EntityManagerFactories {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private EntityManagerFactories() {
  }

  // 1. META-INF/persistence.xml, unit "coffee-shop"
  public static EntityManagerFactory fromPersistenceXml() {
    return Persistence.createEntityManagerFactory("coffee-shop");
  }

  // 2. persistence.xml plus properties that override the file at runtime
  public static EntityManagerFactory fromPersistenceXmlWithOverrides() {
    Map<String, Object> overrides = Map.of(
        "jakarta.persistence.jdbc.url", "jdbc:h2:mem:menu-test",
        "hibernate.show_sql", "false");
    return Persistence.createEntityManagerFactory("coffee-shop", overrides);
  }

  // 3. Jakarta Persistence 3.2 PersistenceConfiguration, no XML, works with any provider
  public static EntityManagerFactory withPersistenceConfiguration() {
    return new PersistenceConfiguration("coffee-shop")
        .managedClass(Drink.class)
        .property(PersistenceConfiguration.JDBC_URL, "jdbc:h2:mem:menu-jpa")
        .property(PersistenceConfiguration.JDBC_USER, "sa")
        .property(PersistenceConfiguration.JDBC_PASSWORD, "")
        .property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, "drop-and-create")
        .property("hibernate.show_sql", true)
        .createEntityManagerFactory();
  }

  // 4. Hibernate's subclass with typed methods; returns a SessionFactory
  public static SessionFactory withHibernatePersistenceConfiguration() {
    return new HibernatePersistenceConfiguration("coffee-shop")
        .managedClass(Drink.class)
        .jdbcUrl("jdbc:h2:mem:menu-hibernate")
        .jdbcCredentials("sa", "")
        .jdbcPoolSize(5)
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(true, false, false)
        .createEntityManagerFactory();
  }

  // 5. Container bootstrap: the framework builds a PersistenceUnitInfo and a DataSource
  public static EntityManagerFactory fromPersistenceUnitInfo() {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:menu-container;DB_CLOSE_DELAY=-1");
    dataSource.setUser("sa");

    return new HibernatePersistenceProvider()
        .createContainerEntityManagerFactory(new CoffeeShopUnitInfo(dataSource), Map.of());
  }

  public static String jdbcUrl(EntityManagerFactory emf) {
    return (String) emf.getProperties().get("jakarta.persistence.jdbc.url");
  }
}
