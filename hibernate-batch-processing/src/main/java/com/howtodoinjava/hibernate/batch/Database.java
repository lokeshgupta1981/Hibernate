package com.howtodoinjava.hibernate.batch;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  /**
   * Creates an EntityManagerFactory on an H2 database. Every JDBC call goes through a
   * datasource-proxy that reports to the counter.
   *
   * @param jdbcUrl  for example jdbc:h2:mem:sensors;DB_CLOSE_DELAY=-1
   * @param settings extra Hibernate settings, for example hibernate.jdbc.batch_size
   */
  public static EntityManagerFactory create(String jdbcUrl, JdbcCounter counter, boolean showSql,
      Map<String, Object> settings) {
    JdbcDataSource h2 = new JdbcDataSource();
    h2.setURL(jdbcUrl);
    h2.setUser("sa");
    h2.setPassword("");

    var dataSource = ProxyDataSourceBuilder.create(h2)
        .name("sensors")
        .listener(counter)
        .build();

    HibernatePersistenceConfiguration config = new HibernatePersistenceConfiguration("sensor-import")
        .managedClasses(Sensor.class, SensorReading.class, LegacyReading.class)
        .property("hibernate.connection.datasource", dataSource)
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false);
    settings.forEach(config::property);
    return config.createEntityManagerFactory();
  }

  public static long count(EntityManagerFactory emf, String entity) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from " + entity, Long.class).getSingleResult());
  }
}
