package com.howtodoinjava.hibernate.saveorupdate.legacy;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/** Hibernate 6.6 has no HibernatePersistenceConfiguration, so this module uses the classic Configuration API. */
public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static SessionFactory create() {
    return new Configuration()
        .addAnnotatedClass(Plant.class)
        .setProperty("hibernate.connection.url", "jdbc:h2:mem:nursery6;DB_CLOSE_DELAY=-1")
        .setProperty("hibernate.connection.username", "sa")
        .setProperty("hibernate.connection.password", "")
        .setProperty("hibernate.hbm2ddl.auto", "create-drop")
        .setProperty("hibernate.session_factory.statement_inspector", SqlLog.class.getName())
        .buildSessionFactory();
  }

  public static long count(SessionFactory sf) {
    return sf.fromTransaction(s -> s.createQuery("select count(*) from Plant", Long.class).getSingleResult());
  }
}
