package com.howtodoinjava.hibernate.states;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return create(showSql, new ArrayList<>());
  }

  /** Same as create(boolean), and records every SQL statement in sqlLog. */
  public static EntityManagerFactory create(boolean showSql, List<String> sqlLog) {
    StatementInspector recorder = sql -> {
      sqlLog.add(sql);
      return sql;
    };
    return new HibernatePersistenceConfiguration("entity-states")
        .property(AvailableSettings.STATEMENT_INSPECTOR, recorder)
        .managedClasses(ServiceOrder.class)
        .jdbcUrl("jdbc:h2:mem:garage;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  /** Works out the lifecycle state of an entity object for the given EntityManager. */
  public static EntityState stateOf(EntityManager em, Object entity) {
    if (em.contains(entity)) {
      return EntityState.MANAGED;
    }
    EntityEntry entry = em.unwrap(SharedSessionContractImplementor.class)
        .getPersistenceContextInternal().getEntry(entity);
    if (entry != null && entry.getStatus() == Status.DELETED) {
      return EntityState.REMOVED;
    }
    Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
    return id == null ? EntityState.TRANSIENT : EntityState.DETACHED;
  }

  public static long count(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from ServiceOrder", Long.class).getSingleResult());
  }

  public static ServiceOrder load(EntityManagerFactory emf, Long id) {
    return emf.callInTransaction(em -> em.find(ServiceOrder.class, id));
  }
}
