package com.howtodoinjava.hibernate.sessionfactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.junit.jupiter.api.Test;

class SessionFactoryBootstrapTest {

  private static void assertWorks(Supplier<SessionFactory> builder, String expectedUrl) {
    try (SessionFactory sessionFactory = builder.get()) {
      sessionFactory.inTransaction(session ->
          session.persist(new Task("Write release notes", "open", LocalDate.of(2026, 10, 9))));
      List<Task> tasks = sessionFactory.fromTransaction(session ->
          session.createSelectionQuery("from Task", Task.class).getResultList());
      assertEquals(1, tasks.size());
      assertEquals("Write release notes", tasks.getFirst().getTitle());
      assertEquals(LocalDate.of(2026, 10, 9), tasks.getFirst().getDueDate());
      assertEquals(expectedUrl, SessionFactories.jdbcUrl(sessionFactory));
    }
  }

  @Test
  void hibernatePersistenceConfigurationBuildsSessionFactory() {
    assertWorks(SessionFactories::withPersistenceConfiguration, "jdbc:h2:mem:taskboard-jpa");
  }

  @Test
  void nativeBootstrapBuildsSessionFactoryWithStatistics() {
    assertWorks(SessionFactories::withNativeBootstrap, "jdbc:h2:mem:taskboard-native");
    try (SessionFactory sessionFactory = SessionFactories.withNativeBootstrap()) {
      assertTrue(sessionFactory.getStatistics().isStatisticsEnabled());
      sessionFactory.inTransaction(session ->
          session.persist(new Task("Fix login bug", "open", LocalDate.of(2026, 10, 9))));
      assertEquals(1, sessionFactory.getStatistics().getEntityInsertCount());
    }
  }

  @Test
  void configurationClassStillWorksAndIsNotDeprecated() {
    assertWorks(SessionFactories::withConfiguration, "jdbc:h2:mem:taskboard-cfg");
    assertFalse(Configuration.class.isAnnotationPresent(Deprecated.class));
  }

  @Test
  void oldBootstrapClassesAreGone() {
    assertThrows(ClassNotFoundException.class,
        () -> Class.forName("org.hibernate.cfg.AnnotationConfiguration"));
    assertThrows(ClassNotFoundException.class,
        () -> Class.forName("org.hibernate.service.ServiceRegistryBuilder"));
  }

  @Test
  void hibernatePropertiesIsReadWithoutAnySettingInCode() {
    assertWorks(SessionFactories::fromHibernateProperties, "jdbc:h2:mem:taskboard");
  }

  @Test
  void hibernatePropertiesIsAlsoReadByHibernatePersistenceConfiguration() {
    assertWorks(() -> new HibernatePersistenceConfiguration("taskboard")
        .managedClass(Task.class)
        .createEntityManagerFactory(), "jdbc:h2:mem:taskboard");
  }

  @Test
  void hibernatePropertiesIsAlsoReadByNativeBootstrapAndPersistenceConfiguration() {
    assertWorks(() -> new MetadataSources(new StandardServiceRegistryBuilder().build())
        .addAnnotatedClass(Task.class)
        .buildMetadata()
        .buildSessionFactory(), "jdbc:h2:mem:taskboard");
    assertWorks(() -> new PersistenceConfiguration("taskboard")
        .managedClass(Task.class)
        .createEntityManagerFactory()
        .unwrap(SessionFactory.class), "jdbc:h2:mem:taskboard");
  }

  @Test
  void hibernateCfgXmlWithConfiguration() {
    assertWorks(SessionFactories::fromCfgXml, "jdbc:h2:mem:taskboard-xml");
  }

  @Test
  void settingInCodeOverridesHibernateCfgXml() {
    assertWorks(() -> new Configuration()
        .configure()
        .setProperty("jakarta.persistence.jdbc.url", "jdbc:h2:mem:taskboard-code")
        .buildSessionFactory(), "jdbc:h2:mem:taskboard-code");
  }

  @Test
  void hibernateCfgXmlWithNativeBootstrap() {
    assertWorks(SessionFactories::fromCfgXmlNative, "jdbc:h2:mem:taskboard-xml");
  }

  @Test
  void unwrapFromPersistenceConfiguration() {
    assertWorks(SessionFactories::fromPersistenceConfiguration, "jdbc:h2:mem:taskboard-unwrap");
  }

  @Test
  void unwrapFromPersistenceXml() {
    assertWorks(SessionFactories::fromPersistenceXml, "jdbc:h2:mem:taskboard-emf");
  }

  @Test
  void mapPassedInCodeOverridesPersistenceXml() {
    assertWorks(() -> Persistence.createEntityManagerFactory("taskboard",
            Map.of("jakarta.persistence.jdbc.url", "jdbc:h2:mem:taskboard-override"))
        .unwrap(SessionFactory.class), "jdbc:h2:mem:taskboard-override");
  }

  @Test
  void unwrapReturnsTheSameObject() {
    try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("taskboard")) {
      SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
      assertSame(emf, sessionFactory);
      assertInstanceOf(SessionFactory.class, emf);
      try (EntityManager em = emf.createEntityManager()) {
        assertInstanceOf(Session.class, em);
        assertSame(em, em.unwrap(Session.class));
      }
    }
  }

  @Test
  void oneSessionFactoryServesManyThreads() throws Exception {
    SessionFactory sessionFactory = TaskBoardDatabase.sessionFactory();
    assertSame(sessionFactory, TaskBoardDatabase.sessionFactory());
    try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
      for (String title : List.of("Plan sprint", "Fix login bug", "Update docs", "Review PR", "Deploy")) {
        pool.submit(() -> sessionFactory.inTransaction(session ->
            session.persist(new Task(title, "open", LocalDate.of(2026, 10, 9)))));
      }
    }
    long count = sessionFactory.fromTransaction(session ->
        session.createSelectionQuery("select count(*) from Task", Long.class).getSingleResult());
    assertEquals(5, count);
  }

  @Test
  void openSessionWithTryWithResources() {
    try (SessionFactory sessionFactory = SessionFactories.withPersistenceConfiguration()) {
      Session leaked;
      try (Session session = sessionFactory.openSession()) {
        session.beginTransaction();
        session.persist(new Task("Review PR", "open", LocalDate.of(2026, 10, 9)));
        session.getTransaction().commit();
        leaked = session;
      }
      assertFalse(leaked.isOpen());
      long count = sessionFactory.fromTransaction(session ->
          session.createSelectionQuery("select count(*) from Task", Long.class).getSingleResult());
      assertEquals(1, count);
    }
  }

  @Test
  void twoSessionFactoriesForTwoDatabasesWorkSideBySide() {
    try (SessionFactory first = SessionFactories.withPersistenceConfiguration();
         SessionFactory second = SessionFactories.withConfiguration()) {
      first.inTransaction(session ->
          session.persist(new Task("Plan sprint", "open", LocalDate.of(2026, 10, 9))));
      long inSecond = second.fromTransaction(session ->
          session.createSelectionQuery("select count(*) from Task", Long.class).getSingleResult());
      assertEquals(0, inSecond);
      assertFalse(first == second);
    }
  }

  @Test
  void getCurrentSessionNeedsASessionContext() {
    try (SessionFactory sessionFactory = SessionFactories.withPersistenceConfiguration()) {
      HibernateException e = assertThrows(HibernateException.class, sessionFactory::getCurrentSession);
      assertEquals("No CurrentSessionContext configured", e.getMessage());
    }
  }

  @Test
  void threadContextReturnsTheSameSessionUntilCommit() {
    try (SessionFactory sessionFactory = SessionFactories.withThreadSessionContext()) {
      Session first = sessionFactory.getCurrentSession();
      first.beginTransaction();
      first.persist(new Task("Plan sprint", "open", LocalDate.of(2026, 10, 9)));
      assertSame(first, sessionFactory.getCurrentSession());
      first.getTransaction().commit();
      assertFalse(first.isOpen());
    }
  }

  @Test
  void closedSessionFactoryRejectsNewSessions() {
    SessionFactory sessionFactory = SessionFactories.withPersistenceConfiguration();
    sessionFactory.close();
    assertTrue(sessionFactory.isClosed());
    IllegalStateException e = assertThrows(IllegalStateException.class, sessionFactory::openSession);
    assertEquals("EntityManagerFactory is closed", e.getMessage());
  }
}
