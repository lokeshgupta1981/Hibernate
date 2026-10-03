package com.howtodoinjava.hibernate.lob;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  static final Class<?>[] ENTITIES = {
      JobApplication.class, ApplicationFile.class, JobPosting.class, ApplicationDraft.class};

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("lob-mapping")
        .managedClasses(ENTITIES)
        .jdbcUrl("jdbc:h2:mem:jobs;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  /**
   * Writes the CREATE TABLE statements Hibernate generates for another database, without connecting to it.
   */
  public static String ddlFor(String productName, int majorVersion) {
    try {
      Path script = Files.createTempFile("ddl-", ".sql");
      Files.delete(script);
      try (EntityManagerFactory emf = new HibernatePersistenceConfiguration("ddl-" + productName)
          .managedClasses(ENTITIES)
          .property("hibernate.boot.allow_jdbc_metadata_access", "false")
          .property("jakarta.persistence.database-product-name", productName)
          .property("jakarta.persistence.database-major-version", String.valueOf(majorVersion))
          .property("jakarta.persistence.schema-generation.scripts.action", "create")
          .property("jakarta.persistence.schema-generation.scripts.create-target", script.toString())
          .createEntityManagerFactory()) {
        // generation runs during bootstrap
      }
      String ddl = Files.readString(script);
      Files.delete(script);
      return ddl;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
