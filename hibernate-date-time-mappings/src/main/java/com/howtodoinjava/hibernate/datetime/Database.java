package com.howtodoinjava.hibernate.datetime;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return create("webinars", showSql, Map.of());
  }

  /** settings: extra Hibernate settings, for example hibernate.jdbc.time_zone. */
  public static EntityManagerFactory create(String dbName, boolean showSql, Map<String, String> settings) {
    HibernatePersistenceConfiguration config = new HibernatePersistenceConfiguration("date-time")
        .managedClasses(Webinar.class, Broadcast.class, WebinarSeries.class)
        .jdbcUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, true, false);
    settings.forEach(config::property);
    return config.createEntityManagerFactory();
  }

  /** Column name to SQL data type, as H2 reports it in INFORMATION_SCHEMA. */
  public static String columnType(EntityManagerFactory emf, String table, String column) {
    return emf.callInTransaction(em -> (String) em.createNativeQuery("""
            select data_type from information_schema.columns
            where table_name = ?1 and column_name = ?2""")
        .setParameter(1, table.toUpperCase())
        .setParameter(2, column.toUpperCase())
        .getSingleResult());
  }

  /** The stored value of each column, cast to text, so we see what the database holds. */
  @SuppressWarnings("unchecked")
  public static List<String> raw(EntityManagerFactory emf, String table, Long id, String... columns) {
    StringBuilder sql = new StringBuilder("select ");
    for (int i = 0; i < columns.length; i++) {
      sql.append(i > 0 ? ", " : "").append("cast(").append(columns[i]).append(" as varchar)");
    }
    sql.append(" from ").append(table).append(" where id = ?1");
    return emf.callInTransaction(em -> {
      Object result = em.createNativeQuery(sql.toString()).setParameter(1, id).getSingleResult();
      if (result instanceof Object[] row) {
        return java.util.Arrays.stream(row).map(String::valueOf).toList();
      }
      return List.of(String.valueOf(result));
    });
  }
}
