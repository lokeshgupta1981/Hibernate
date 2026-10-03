package com.howtodoinjava.hibernate.c3p0;

import com.mchange.v2.c3p0.PoolBackedDataSource;
import com.mchange.v2.c3p0.PooledDataSource;
import com.mchange.v2.c3p0.WrapperConnectionPoolDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import javax.sql.DataSource;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  public static final String URL = "jdbc:h2:mem:garage;DB_CLOSE_DELAY=-1";

  private static final AtomicInteger DATABASES = new AtomicInteger();

  /** The c3p0 settings used in the article. */
  public static final Map<String, Object> C3P0_SETTINGS = Map.of(
      "hibernate.c3p0.min_size", 5,
      "hibernate.c3p0.max_size", 20,
      "hibernate.c3p0.acquire_increment", 5,
      "hibernate.c3p0.timeout", 300,
      "hibernate.c3p0.idle_test_period", 30,
      "hibernate.c3p0.max_statements", 50);

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
    System.setProperty("com.mchange.v2.log.MLog", "slf4j");
  }

  private Database() {
  }

  /** Forces Hibernate's built-in pool, which is what we get without any pool module. */
  public static EntityManagerFactory builtIn(boolean showSql) {
    return create(newUrl(), Map.of("hibernate.connection.provider_class",
        "org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider"), showSql);
  }

  /** hibernate-c3p0 is on the classpath, so the hibernate.c3p0.* settings configure c3p0. */
  public static EntityManagerFactory c3p0(Map<String, Object> settings, boolean showSql) {
    return create(newUrl(), settings, showSql);
  }

  /** A fresh in-memory database for every EntityManagerFactory. */
  public static String newUrl() {
    return "jdbc:h2:mem:garage" + DATABASES.incrementAndGet() + ";DB_CLOSE_DELAY=-1";
  }

  public static EntityManagerFactory create(String url, Map<String, Object> settings, boolean showSql) {
    HibernatePersistenceConfiguration cfg = new HibernatePersistenceConfiguration("garage")
        .managedClasses(ParkingEntry.class)
        .jdbcDriver("org.h2.Driver")
        .jdbcUrl(url)
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false);
    settings.forEach(cfg::property);
    return cfg.createEntityManagerFactory();
  }

  public static ConnectionProvider connectionProvider(EntityManagerFactory emf) {
    return emf.unwrap(SessionFactoryImplementor.class)
        .getServiceRegistry()
        .requireService(ConnectionProvider.class);
  }

  /** The c3p0 pool behind Hibernate. */
  public static PooledDataSource pool(EntityManagerFactory emf) {
    return (PooledDataSource) connectionProvider(emf).unwrap(DataSource.class);
  }

  /** The settings c3p0 is running with (min/max size, timeouts, tests). */
  public static WrapperConnectionPoolDataSource config(EntityManagerFactory emf) {
    return (WrapperConnectionPoolDataSource) ((PoolBackedDataSource) pool(emf)).getConnectionPoolDataSource();
  }

  /** Hibernate gets a ready DataSource, as in Spring Boot. */
  public static EntityManagerFactory withDataSource(DataSource dataSource, Map<String, Object> settings) {
    HibernatePersistenceConfiguration cfg = new HibernatePersistenceConfiguration("garage")
        .managedClasses(ParkingEntry.class)
        .property("hibernate.connection.datasource", dataSource)
        .schemaToolingAction(Action.CREATE_DROP);
    settings.forEach(cfg::property);
    return cfg.createEntityManagerFactory();
  }

  public static int total(PooledDataSource pool) {
    try {
      return pool.getNumConnectionsDefaultUser();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public static int busy(PooledDataSource pool) {
    try {
      return pool.getNumBusyConnectionsDefaultUser();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public static int idle(PooledDataSource pool) {
    try {
      return pool.getNumIdleConnectionsDefaultUser();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String stats(PooledDataSource pool) {
    return "total=" + total(pool) + ", busy=" + busy(pool) + ", idle=" + idle(pool);
  }

  /** Opens n EntityManagers, each with an active transaction, so each holds one connection. */
  public static List<EntityManager> holdConnections(EntityManagerFactory emf, int n) {
    List<EntityManager> ems = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      EntityManager em = emf.createEntityManager();
      em.getTransaction().begin();
      ems.add(em);
    }
    return ems;
  }

  public static void release(List<EntityManager> ems) {
    for (EntityManager em : ems) {
      if (em.getTransaction().isActive()) {
        em.getTransaction().rollback();
      }
      em.close();
    }
  }

  /** c3p0 grows and shrinks the pool in background threads, so we poll. */
  public static boolean await(BooleanSupplier condition, Duration timeout) {
    long end = System.nanoTime() + timeout.toNanos();
    while (System.nanoTime() < end) {
      if (condition.getAsBoolean()) {
        return true;
      }
      LockSupport.parkNanos(Duration.ofMillis(100).toNanos());
    }
    return condition.getAsBoolean();
  }

  public static long countEntries(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from ParkingEntry", Long.class).getSingleResult());
  }

  /** Runs the H2 SHUTDOWN command; every pooled connection to this database is now broken. */
  public static void shutdown(EntityManagerFactory emf) {
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      em.createNativeQuery("SHUTDOWN").executeUpdate();
    } catch (RuntimeException expected) {
      // the connection is closed by the shutdown itself
    } finally {
      try {
        em.close();
      } catch (RuntimeException ignored) {
        // the transaction cannot be rolled back on a closed connection
      }
    }
  }
}
