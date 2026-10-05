package com.howtodoinjava.hibernate.onetomany;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /** Creates an in-memory H2 database with the given entities and records every SQL in SqlLog. */
  public static EntityManagerFactory create(String name, boolean showSql, Map<String, Object> extra,
      Class<?>... entities) {
    HibernatePersistenceConfiguration config = new HibernatePersistenceConfiguration(name)
        .managedClasses(entities)
        .jdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.session_factory.statement_inspector", SqlLog.class.getName());
    extra.forEach(config::property);
    return config.createEntityManagerFactory();
  }

  public static EntityManagerFactory create(String name, boolean showSql, Class<?>... entities) {
    return create(name, showSql, Map.of(), entities);
  }

  public static long count(EntityManagerFactory emf, String entity) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from " + entity, Long.class).getSingleResult());
  }
}
