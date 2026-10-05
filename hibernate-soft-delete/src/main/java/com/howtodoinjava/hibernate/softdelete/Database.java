package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /** The bookmarks model: BookmarkFolder and Bookmark. */
  public static EntityManagerFactory create(boolean showSql) {
    return create("bookmarks", showSql, BookmarkFolder.class, Bookmark.class);
  }

  /** One in-memory H2 database per name, so each mapping variant gets its own schema. */
  public static EntityManagerFactory create(String name, boolean showSql, Class<?>... entities) {
    return create(name, showSql, (StatementInspector) null, entities);
  }

  /** Same as above; the inspector (if not null) sees every SQL statement, for the tests. */
  public static EntityManagerFactory create(String name, boolean showSql, StatementInspector inspector,
      Class<?>... entities) {
    HibernatePersistenceConfiguration config = new HibernatePersistenceConfiguration(name);
    if (inspector != null) {
      config.property(AvailableSettings.STATEMENT_INSPECTOR, inspector);
    }
    return config
        .managedClasses(entities)
        .jdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  /** Rows in a table as native SQL sees them, soft-deleted rows included. */
  public static List<?> nativeRows(EntityManagerFactory emf, String sql) {
    return emf.callInTransaction(em -> em.createNativeQuery(sql).getResultList());
  }
}
