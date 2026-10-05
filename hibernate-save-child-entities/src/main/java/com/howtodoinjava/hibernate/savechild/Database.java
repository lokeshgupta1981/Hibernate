package com.howtodoinjava.hibernate.savechild;

import jakarta.persistence.EntityManagerFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /** Main model: Survey and Question with cascade PERSIST and MERGE. */
  public static EntityManagerFactory create(boolean showSql, SqlLog log) {
    return create(showSql, log, Survey.class, Question.class);
  }

  public static EntityManagerFactory create(boolean showSql, SqlLog log, Class<?>... entities) {
    return new HibernatePersistenceConfiguration("save-child")
        .managedClasses(entities)
        .jdbcUrl("jdbc:h2:mem:surveys" + COUNTER.incrementAndGet() + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property(AvailableSettings.STATEMENT_INSPECTOR, log)
        .createEntityManagerFactory();
  }

  public static long count(EntityManagerFactory emf, String entity) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from " + entity, Long.class).getSingleResult());
  }
}
