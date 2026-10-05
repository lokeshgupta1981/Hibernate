package com.howtodoinjava.hibernate.c3p0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.PooledDataSource;
import com.mchange.v2.c3p0.WrapperConnectionPoolDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.engine.jdbc.connections.internal.DataSourceConnectionProvider;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.exception.GenericJDBCException;
import org.junit.jupiter.api.Test;

class C3p0PoolTest {

  @Test
  void builtInPoolWhenForced() {
    try (EntityManagerFactory emf = Database.builtIn(false)) {
      assertInstanceOf(DriverManagerConnectionProvider.class, Database.connectionProvider(emf));
    }
  }

  @Test
  void c3p0IsUsedWhenHibernateC3p0IsTheOnlyPoolOnTheClasspath() {
    try (EntityManagerFactory emf = Database.c3p0(Map.of(), false)) {
      assertInstanceOf(C3P0ConnectionProvider.class, Database.connectionProvider(emf));
      WrapperConnectionPoolDataSource pool = Database.config(emf);
      assertEquals(3, pool.getMinPoolSize());
      assertEquals(15, pool.getMaxPoolSize());
      assertEquals(3, pool.getAcquireIncrement());
      assertEquals(0, pool.getMaxIdleTime());
      assertEquals(0, pool.getIdleConnectionTestPeriod());
      assertEquals(0, pool.getMaxStatements());
    }
  }

  @Test
  void providerClassByShortName() {
    try (EntityManagerFactory emf = Database.c3p0(Map.of("hibernate.connection.provider_class", "c3p0"), false)) {
      assertInstanceOf(C3P0ConnectionProvider.class, Database.connectionProvider(emf));
    }
  }

  @Test
  void oldProviderClassNameIsStillAcceptedAsAlias() {
    try (EntityManagerFactory emf = Database.c3p0(
        Map.of("hibernate.connection.provider_class", "org.hibernate.connection.C3P0ConnectionProvider"), false)) {
      assertInstanceOf(C3P0ConnectionProvider.class, Database.connectionProvider(emf));
    }
  }

  @Test
  void hibernateSettingsReachTheC3p0Pool() {
    try (EntityManagerFactory emf = Database.c3p0(Database.C3P0_SETTINGS, false)) {
      WrapperConnectionPoolDataSource pool = Database.config(emf);
      assertEquals(5, pool.getMinPoolSize());
      assertEquals(5, pool.getInitialPoolSize());
      assertEquals(20, pool.getMaxPoolSize());
      assertEquals(5, pool.getAcquireIncrement());
      assertEquals(300, pool.getMaxIdleTime());
      assertEquals(30, pool.getIdleConnectionTestPeriod());
      assertEquals(50, pool.getMaxStatements());
      PooledDataSource stats = Database.pool(emf);
      assertTrue(Database.await(() -> Database.busy(stats) == 0, Duration.ofSeconds(5)));
      assertEquals(5, Database.total(stats));
    }
  }

  @Test
  void settingsFromPersistenceXml() {
    try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("garage")) {
      assertInstanceOf(C3P0ConnectionProvider.class, Database.connectionProvider(emf));
      WrapperConnectionPoolDataSource pool = Database.config(emf);
      assertEquals(5, pool.getMinPoolSize());
      assertEquals(20, pool.getMaxPoolSize());
      assertEquals(300, pool.getMaxIdleTime());
    }
  }

  @Test
  void c3p0PropertyNamesArePassedThrough() {
    Map<String, Object> settings = Map.of(
        "hibernate.c3p0.checkoutTimeout", 1000,
        "hibernate.c3p0.testConnectionOnCheckin", true,
        "hibernate.c3p0.maxIdleTimeExcessConnections", 60,
        "hibernate.c3p0.unreturnedConnectionTimeout", 30);
    try (EntityManagerFactory emf = Database.c3p0(settings, false)) {
      WrapperConnectionPoolDataSource pool = Database.config(emf);
      assertEquals(1000, pool.getCheckoutTimeout());
      assertTrue(pool.isTestConnectionOnCheckin());
      assertEquals(60, pool.getMaxIdleTimeExcessConnections());
      assertEquals(30, pool.getUnreturnedConnectionTimeout());
    }
  }

  @Test
  void poolSizeIsUsedAsMaxSizeWhenMaxSizeIsMissing() {
    try (EntityManagerFactory emf = Database.c3p0(
        Map.of("hibernate.c3p0.min_size", 2, "hibernate.connection.pool_size", 8), false)) {
      WrapperConnectionPoolDataSource pool = Database.config(emf);
      assertEquals(8, pool.getMaxPoolSize());
    }
  }

  @Test
  void aTransactionBorrowsOneConnectionAndReturnsItAtCommit() {
    try (EntityManagerFactory emf = Database.c3p0(Database.C3P0_SETTINGS, false)) {
      PooledDataSource pool = Database.pool(emf);
      Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5));
      int[] busyInside = new int[1];
      emf.runInTransaction(em -> {
        em.persist(new ParkingEntry("ab-123", LocalDateTime.of(2026, 10, 3, 8, 15)));
        em.persist(new ParkingEntry("cd-456", LocalDateTime.of(2026, 10, 3, 8, 40)));
        em.flush();
        busyInside[0] = Database.busy(pool);
      });
      assertEquals(1, busyInside[0]);
      assertTrue(Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5)));
      assertEquals(5, Database.total(pool));
      assertEquals(2, Database.countEntries(emf));
    }
  }

  @Test
  void poolGrowsByAcquireIncrement() {
    try (EntityManagerFactory emf = Database.c3p0(Database.C3P0_SETTINGS, false)) {
      PooledDataSource pool = Database.pool(emf);
      Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5));
      List<EntityManager> held = Database.holdConnections(emf, 7);
      assertTrue(Database.await(() -> Database.total(pool) == 10, Duration.ofSeconds(5)));
      assertTrue(Database.await(() -> Database.busy(pool) == 7, Duration.ofSeconds(1)));
      Database.release(held);
      assertTrue(Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5)));
      assertEquals(10, Database.total(pool));
    }
  }

  @Test
  void exhaustedPoolTimesOutAfterCheckoutTimeout() {
    Map<String, Object> small = Map.of(
        "hibernate.c3p0.min_size", 1,
        "hibernate.c3p0.max_size", 2,
        "hibernate.c3p0.checkoutTimeout", 1000);
    try (EntityManagerFactory emf = Database.c3p0(small, false)) {
      List<EntityManager> held = Database.holdConnections(emf, 2);
      long start = System.nanoTime();
      GenericJDBCException e = assertThrows(GenericJDBCException.class, () -> Database.holdConnections(emf, 1));
      long waited = Duration.ofNanos(System.nanoTime() - start).toMillis();
      assertTrue(e.getMessage().contains("An attempt by a client to checkout a Connection has timed out."));
      assertTrue(waited >= 900 && waited < 3000, "waited " + waited);
      Database.release(held);
    }
  }

  @Test
  void excessIdleConnectionsAreClosed() {
    Map<String, Object> shrinking = Map.of(
        "hibernate.c3p0.min_size", 2,
        "hibernate.c3p0.max_size", 10,
        "hibernate.c3p0.acquire_increment", 2,
        "hibernate.c3p0.maxIdleTimeExcessConnections", 2);
    try (EntityManagerFactory emf = Database.c3p0(shrinking, false)) {
      PooledDataSource pool = Database.pool(emf);
      List<EntityManager> held = Database.holdConnections(emf, 6);
      assertTrue(Database.total(pool) >= 6);
      Database.release(held);
      assertTrue(Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5)));
      assertTrue(Database.await(() -> Database.total(pool) == 2, Duration.ofSeconds(15)));
    }
  }

  @Test
  void unreturnedConnectionIsDestroyedAfterTimeout() {
    Map<String, Object> leak = Map.of("hibernate.c3p0.unreturnedConnectionTimeout", 2);
    try (EntityManagerFactory emf = Database.c3p0(leak, false)) {
      PooledDataSource pool = Database.pool(emf);
      Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(5));
      EntityManager forgotten = emf.createEntityManager();
      forgotten.getTransaction().begin();
      assertTrue(Database.await(() -> Database.busy(pool) == 1, Duration.ofSeconds(1)));
      assertTrue(Database.await(() -> Database.busy(pool) == 0, Duration.ofSeconds(15)));
      assertTrue(Database.await(() -> Database.total(pool) == 3, Duration.ofSeconds(10)));
    }
  }

  @Test
  void brokenConnectionsFailWithoutTesting() {
    assertFalse(C3p0Demo.restartAndCount(false) >= 0);
  }

  @Test
  void testConnectionOnCheckoutReplacesBrokenConnections() {
    assertEquals(1, C3p0Demo.restartAndCount(true));
  }

  @Test
  void hibernateC3p0SettingsAreIgnoredWhenADataSourceIsGiven() throws Exception {
    try (ComboPooledDataSource ds = new ComboPooledDataSource()) {
      ds.setJdbcUrl(Database.newUrl());
      ds.setUser("sa");
      ds.setMaxPoolSize(20);
      try (EntityManagerFactory emf = Database.withDataSource(ds, Map.of("hibernate.c3p0.max_size", 99))) {
        assertInstanceOf(DataSourceConnectionProvider.class, Database.connectionProvider(emf));
        assertEquals(20, ds.getMaxPoolSize());
      }
    }
  }
}
