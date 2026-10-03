package com.howtodoinjava.hibernate.sessionfactory;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class SessionFactoryDemo {

  public static void main(String[] args) throws Exception {
    run("1. HibernatePersistenceConfiguration", SessionFactories::withPersistenceConfiguration);
    run("2. Native bootstrap (StandardServiceRegistryBuilder + MetadataSources)",
        SessionFactories::withNativeBootstrap);
    run("3. Configuration class", SessionFactories::withConfiguration);
    run("4a. hibernate.properties only", SessionFactories::fromHibernateProperties);
    run("4b. hibernate.cfg.xml with Configuration", SessionFactories::fromCfgXml);
    run("4c. hibernate.cfg.xml with StandardServiceRegistryBuilder", SessionFactories::fromCfgXmlNative);
    run("5a. Unwrap from PersistenceConfiguration", SessionFactories::fromPersistenceConfiguration);
    run("5b. Unwrap from persistence.xml", SessionFactories::fromPersistenceXml);

    step("6. One SessionFactory, five threads, one Session per thread");
    SessionFactory sessionFactory = TaskBoardDatabase.sessionFactory();
    try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
      for (String title : List.of("Plan sprint", "Fix login bug", "Update docs", "Review PR", "Deploy")) {
        pool.submit(() -> sessionFactory.inTransaction(session ->
            session.persist(new Task(title, "open", LocalDate.of(2026, 10, 9)))));
      }
    }
    long count = sessionFactory.fromTransaction(session ->
        session.createSelectionQuery("select count(*) from Task", Long.class).getSingleResult());
    System.out.println("tasks = " + count);
    System.out.println("same factory = " + (sessionFactory == TaskBoardDatabase.sessionFactory()));

    step("7. getCurrentSession() without a session context");
    try {
      sessionFactory.getCurrentSession();
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
    }

    step("8. getCurrentSession() with hibernate.current_session_context_class=thread");
    try (SessionFactory threadBound = SessionFactories.withThreadSessionContext()) {
      Session first = threadBound.getCurrentSession();
      first.beginTransaction();
      first.persist(new Task("Plan sprint", "open", LocalDate.of(2026, 10, 9)));
      Session second = threadBound.getCurrentSession();
      System.out.println("same session = " + (first == second));
      second.getTransaction().commit();
      System.out.println("open after commit = " + first.isOpen());
    }

    step("9. Close the SessionFactory");
    SessionFactory closed = SessionFactories.withPersistenceConfiguration();
    closed.close();
    System.out.println("isClosed = " + closed.isClosed());
    try {
      closed.openSession();
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
    }
  }

  private static void run(String title, Supplier<SessionFactory> builder) {
    step(title);
    try (SessionFactory sessionFactory = builder.get()) {
      sessionFactory.inTransaction(session ->
          session.persist(new Task("Write release notes", "open", LocalDate.of(2026, 10, 9))));
      List<Task> tasks = sessionFactory.fromTransaction(session ->
          session.createSelectionQuery("from Task", Task.class).getResultList());
      System.out.println("tasks = " + tasks);
      System.out.println("url   = " + SessionFactories.jdbcUrl(sessionFactory));
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }
}
