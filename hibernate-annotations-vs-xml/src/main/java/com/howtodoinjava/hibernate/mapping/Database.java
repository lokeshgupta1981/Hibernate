package com.howtodoinjava.hibernate.mapping;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

/**
 * Builds one EntityManagerFactory per mapping style, each on its own in-memory H2 database.
 */
public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /** 1. Mapping from annotations only. */
  public static EntityManagerFactory annotations(boolean showSql) {
    return base("annotations", showSql)
        .managedClass(Bicycle.class)
        .createEntityManagerFactory();
  }

  /** 2. Mapping from orm.xml only; the class has no annotations. */
  public static EntityManagerFactory ormXml(boolean showSql) {
    return base("orm-xml", showSql)
        .mappingFile("META-INF/bicycle-orm.xml")
        .createEntityManagerFactory();
  }

  /** 3. Annotations plus an orm.xml that overrides two of them (metadata-complete="false"). */
  public static EntityManagerFactory annotationsWithOverride(boolean showSql) {
    return base("override", showSql)
        .managedClass(Bicycle.class)
        .mappingFile("META-INF/bicycle-override.xml")
        .createEntityManagerFactory();
  }

  /** 4. Annotations plus an orm.xml with metadata-complete="true": the annotations are ignored. */
  public static EntityManagerFactory metadataComplete(boolean showSql) {
    return base("complete", showSql)
        .managedClass(Bicycle.class)
        .mappingFile("META-INF/bicycle-complete.xml")
        .createEntityManagerFactory();
  }

  /** 5. Legacy hbm.xml through the native Configuration API (deprecated). */
  public static EntityManagerFactory hbmXml(boolean showSql) {
    return new Configuration()
        .addResource("Bicycle.hbm.xml")
        .setProperty("hibernate.connection.url", "jdbc:h2:mem:hbm;DB_CLOSE_DELAY=-1")
        .setProperty("hibernate.connection.username", "sa")
        .setProperty("hibernate.connection.password", "")
        .setProperty("hibernate.hbm2ddl.auto", "create-drop")
        .setProperty("hibernate.show_sql", String.valueOf(showSql))
        .buildSessionFactory();
  }

  private static HibernatePersistenceConfiguration base(String name, boolean showSql) {
    return new HibernatePersistenceConfiguration(name)
        .jdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false);
  }

  /** Columns of a table as "name type", read from the H2 information schema. */
  @SuppressWarnings("unchecked")
  public static List<String> columns(EntityManagerFactory emf, String table) {
    return emf.callInTransaction(em -> ((List<Object[]>) em.createNativeQuery(
            "select column_name, data_type, character_maximum_length, numeric_precision, numeric_scale, is_nullable"
                + " from information_schema.columns where table_name = ?1 order by ordinal_position")
        .setParameter(1, table.toUpperCase())
        .getResultList())
        .stream()
        .map(Database::describe)
        .toList());
  }

  private static String describe(Object[] c) {
    String type = c[1].toString();
    if (type.contains("VARYING")) {
      type = "varchar(" + c[2] + ")";
    } else if (type.equals("NUMERIC")) {
      type = type + "(" + c[3] + "," + c[4] + ")";
    }
    String notNull = "NO".equals(c[5]) ? " not null" : "";
    return c[0].toString().toLowerCase() + " " + type.toLowerCase() + notNull;
  }
}
