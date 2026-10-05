package com.howtodoinjava.hibernate.validator;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("conference")
        .managedClasses(Attendee.class, Address.class)
        .jdbcUrl("jdbc:h2:mem:conference;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  public static long count(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from Attendee", Long.class).getSingleResult());
  }
}
