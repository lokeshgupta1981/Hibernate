package com.howtodoinjava.hibernate.persister;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Map;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  // Registers exactly the classes passed in; Hibernate does not scan anything else
  public static EntityManagerFactory create(boolean showSql, Class<?>... entities) {
    return new HibernatePersistenceConfiguration("ferry")
        .managedClasses(entities)
        .jdbcUrl("jdbc:h2:mem:ferry;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  // Uses a persistence unit from META-INF/persistence.xml
  public static EntityManagerFactory fromPersistenceXml(String unit, boolean showSql) {
    return Persistence.createEntityManagerFactory(unit, Map.of("hibernate.show_sql", String.valueOf(showSql)));
  }
}
