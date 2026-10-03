package com.howtodoinjava.hibernate.manytomany;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(String name, boolean showSql, Class<?>... entities) {
    return new HibernatePersistenceConfiguration(name)
        .managedClasses(entities)
        .jdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.session_factory.statement_inspector", SqlLog.class.getName())
        .createEntityManagerFactory();
  }

  public static long count(EntityManagerFactory emf, String table) {
    return emf.callInTransaction(em -> ((Number) em
        .createNativeQuery("select count(*) from " + table).getSingleResult()).longValue());
  }
}
