package com.howtodoinjava.hibernate.sessionfactory;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

/**
 * Every way to build a SessionFactory in Hibernate 7.4. Each method uses its own in-memory
 * database, so a test can check which settings were applied.
 */
public final class SessionFactories {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private SessionFactories() {
  }

  // 1. Jakarta Persistence 3.2 style: HibernatePersistenceConfiguration
  public static SessionFactory withPersistenceConfiguration() {
    return new HibernatePersistenceConfiguration("taskboard")
        .managedClass(Task.class)
        .jdbcUrl("jdbc:h2:mem:taskboard-jpa")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(true, false, false)
        .createEntityManagerFactory();
  }

  // 2. Native bootstrap: service registry -> metadata -> SessionFactory
  public static SessionFactory withNativeBootstrap() {
    StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
        .applySetting("jakarta.persistence.jdbc.url", "jdbc:h2:mem:taskboard-native")
        .applySetting("jakarta.persistence.jdbc.user", "sa")
        .applySetting("jakarta.persistence.jdbc.password", "")
        .applySetting("hibernate.hbm2ddl.auto", "create-drop")
        .applySetting("hibernate.show_sql", true)
        .build();
    try {
      return new MetadataSources(registry)
          .addAnnotatedClass(Task.class)
          .buildMetadata()
          .getSessionFactoryBuilder()
          .applyStatisticsSupport(true)
          .build();
    } catch (Exception e) {
      StandardServiceRegistryBuilder.destroy(registry);   // release the registry if the build fails
      throw e;
    }
  }

  // 3. The Configuration class with settings in code
  public static SessionFactory withConfiguration() {
    return new Configuration()
        .addAnnotatedClass(Task.class)
        .setProperty("jakarta.persistence.jdbc.url", "jdbc:h2:mem:taskboard-cfg")
        .setCredentials("sa", "")
        .setSchemaExportAction(Action.CREATE_DROP)
        .showSql(true, false, false)
        .buildSessionFactory();
  }

  // 4a. Settings only in hibernate.properties (root of the classpath)
  public static SessionFactory fromHibernateProperties() {
    return new Configuration()
        .addAnnotatedClass(Task.class)
        .buildSessionFactory();
  }

  // 4b. Settings and mappings in hibernate.cfg.xml, read by Configuration
  public static SessionFactory fromCfgXml() {
    return new Configuration()
        .configure()                          // reads /hibernate.cfg.xml
        .buildSessionFactory();
  }

  // 4c. hibernate.cfg.xml read by the native bootstrap
  public static SessionFactory fromCfgXmlNative() {
    StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
        .configure()                          // reads /hibernate.cfg.xml
        .build();
    try {
      return new MetadataSources(registry)
          .buildMetadata()
          .buildSessionFactory();
    } catch (Exception e) {
      StandardServiceRegistryBuilder.destroy(registry);
      throw e;
    }
  }

  // 5a. Unwrap from a standard EntityManagerFactory built with PersistenceConfiguration
  public static SessionFactory fromPersistenceConfiguration() {
    EntityManagerFactory emf = new PersistenceConfiguration("taskboard")
        .managedClass(Task.class)
        .property(PersistenceConfiguration.JDBC_URL, "jdbc:h2:mem:taskboard-unwrap")
        .property(PersistenceConfiguration.JDBC_USER, "sa")
        .property(PersistenceConfiguration.JDBC_PASSWORD, "")
        .property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, "drop-and-create")
        .createEntityManagerFactory();
    return emf.unwrap(SessionFactory.class);
  }

  // 5b. Unwrap from an EntityManagerFactory built from META-INF/persistence.xml
  public static SessionFactory fromPersistenceXml() {
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("taskboard");
    return emf.unwrap(SessionFactory.class);
  }

  // getCurrentSession() needs a session context; "thread" binds one Session to each thread
  public static SessionFactory withThreadSessionContext() {
    return new HibernatePersistenceConfiguration("taskboard")
        .managedClass(Task.class)
        .jdbcUrl("jdbc:h2:mem:taskboard-thread")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .property("hibernate.current_session_context_class", "thread")
        .createEntityManagerFactory();
  }

  public static String jdbcUrl(SessionFactory sessionFactory) {
    return (String) sessionFactory.getProperties().get("jakarta.persistence.jdbc.url");
  }
}
