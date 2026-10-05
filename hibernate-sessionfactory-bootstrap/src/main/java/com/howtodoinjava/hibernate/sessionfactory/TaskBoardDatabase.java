package com.howtodoinjava.hibernate.sessionfactory;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

/**
 * Holds the one SessionFactory of the application. The JVM builds it once, on first use,
 * and the shutdown hook closes it when the application stops.
 */
public final class TaskBoardDatabase {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private static final SessionFactory SESSION_FACTORY = build();

  private TaskBoardDatabase() {
  }

  private static SessionFactory build() {
    SessionFactory sessionFactory = new HibernatePersistenceConfiguration("taskboard")
        .managedClass(Task.class)
        .jdbcUrl("jdbc:h2:mem:taskboard-app")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .createEntityManagerFactory();
    Runtime.getRuntime().addShutdownHook(new Thread(sessionFactory::close));
    return sessionFactory;
  }

  public static SessionFactory sessionFactory() {
    return SESSION_FACTORY;
  }
}
