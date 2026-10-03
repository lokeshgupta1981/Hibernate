package com.howtodoinjava.hibernate.callbacks;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return create(showSql, "META-INF/orm.xml", "claims");
  }

  /** Same entities, plus RiskyListener as a default listener (see META-INF/orm-risky.xml). */
  public static EntityManagerFactory createRisky(boolean showSql) {
    return create(showSql, "META-INF/orm-risky.xml", "risky");
  }

  private static EntityManagerFactory create(boolean showSql, String mappingFile, String dbName) {
    return new HibernatePersistenceConfiguration("entity-callbacks-" + dbName)
        .managedClasses(ExpenseClaim.class, TravelClaim.class, ClaimHistory.class)
        .mappingFile(mappingFile)
        .jdbcUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property(AvailableSettings.STATEMENT_INSPECTOR, SqlRecorder.class.getName())
        .createEntityManagerFactory();
  }
}
