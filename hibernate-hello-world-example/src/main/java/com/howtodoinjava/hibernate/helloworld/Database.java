package com.howtodoinjava.hibernate.helloworld;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /** Configuration in Java code: no XML file needed. */
  public static SessionFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("notes")
        .managedClass(Note.class)
        .jdbcUrl("jdbc:h2:mem:notes")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  /** Configuration in src/main/resources/META-INF/persistence.xml. */
  public static EntityManagerFactory fromPersistenceXml() {
    return Persistence.createEntityManagerFactory("notes");
  }

  /** Legacy configuration in src/main/resources/hibernate.cfg.xml. */
  public static SessionFactory fromHibernateCfgXml() {
    return new Configuration().configure().buildSessionFactory();
  }
}
