package com.howtodoinjava.hibernate.annotations;

import jakarta.persistence.EntityManagerFactory;
import java.nio.file.Path;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return create(showSql, null);
  }

  /** Creates the schema; when ddlFile is not null, the CREATE script is also written to that file. */
  public static EntityManagerFactory create(boolean showSql, Path ddlFile) {
    HibernatePersistenceConfiguration config = new HibernatePersistenceConfiguration("theater")
        .managedClasses(Theater.class, Play.class, Performance.class)
        .jdbcUrl("jdbc:h2:mem:theater;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false);
    if (ddlFile != null) {
      config.property("jakarta.persistence.schema-generation.database.action", "drop-and-create")
          .property("jakarta.persistence.schema-generation.scripts.action", "create")
          .property("jakarta.persistence.schema-generation.scripts.create-target", ddlFile.toString())
          .property("hibernate.format_sql", "true")
          .property("hibernate.hbm2ddl.delimiter", ";");
    }
    return config.createEntityManagerFactory();
  }
}
