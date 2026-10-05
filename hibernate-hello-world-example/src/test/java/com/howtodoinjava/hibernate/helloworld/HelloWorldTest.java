package com.howtodoinjava.hibernate.helloworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HelloWorldTest {

  private static final LocalDate OCT_3 = LocalDate.of(2026, 10, 3);

  private SessionFactory sessionFactory;
  private Long id;

  @BeforeEach
  void setUp() {
    sessionFactory = Database.create(false);
    id = sessionFactory.callInTransaction(em -> {
      Note groceries = new Note("Groceries", "Milk, eggs, bread", OCT_3);
      em.persist(groceries);
      em.persist(new Note("Ideas", "Learn Hibernate", OCT_3));
      return groceries.getId();
    });
  }

  @AfterEach
  void tearDown() {
    sessionFactory.close();
  }

  private long count(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from Note", Long.class).getSingleResult());
  }

  private static String jdbcUrl(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> em.unwrap(Session.class)
        .doReturningWork(connection -> connection.getMetaData().getURL()));
  }

  @Test
  void persistGeneratesTheId() {
    assertEquals(1L, id);
    assertEquals(2, count(sessionFactory));
  }

  @Test
  void findReadsTheRow() {
    Note note = sessionFactory.callInTransaction(em -> em.find(Note.class, id));
    assertEquals("Groceries", note.getTitle());
    assertEquals("Milk, eggs, bread", note.getText());
    assertEquals(OCT_3, note.getCreatedOn());
  }

  @Test
  void queryReturnsAllNotesSortedByTitle() {
    List<String> titles = sessionFactory.callInTransaction(em ->
        em.createQuery("from Note order by title", Note.class).getResultList())
        .stream().map(Note::getTitle).toList();
    assertEquals(List.of("Groceries", "Ideas"), titles);
  }

  @Test
  void changingAManagedEntityUpdatesTheRow() {
    sessionFactory.runInTransaction(em -> em.find(Note.class, id).setText("Milk, eggs, bread, coffee"));
    Note note = sessionFactory.callInTransaction(em -> em.find(Note.class, id));
    assertEquals("Milk, eggs, bread, coffee", note.getText());
  }

  @Test
  void changingADetachedEntityNeedsMerge() {
    Note detached = sessionFactory.callInTransaction(em -> em.find(Note.class, id));
    detached.setTitle("Shopping");
    Note stillOld = sessionFactory.callInTransaction(em -> em.find(Note.class, id));
    assertEquals("Groceries", stillOld.getTitle());

    sessionFactory.runInTransaction(em -> em.merge(detached));
    Note merged = sessionFactory.callInTransaction(em -> em.find(Note.class, id));
    assertEquals("Shopping", merged.getTitle());
  }

  @Test
  void removeDeletesTheRow() {
    sessionFactory.runInTransaction(em -> em.remove(em.find(Note.class, id)));
    assertNull(sessionFactory.callInTransaction(em -> em.find(Note.class, id)));
    assertEquals(1, count(sessionFactory));
  }

  @Test
  void sessionApiWorksOnTheSameFactory() {
    Long todoId = sessionFactory.fromTransaction(session -> {
      Note todo = new Note("Todo", "Call the bank", LocalDate.of(2026, 10, 4));
      session.persist(todo);
      return todo.getId();
    });
    assertEquals(3L, todoId);
    String title = sessionFactory.fromTransaction(session -> session.find(Note.class, todoId).getTitle());
    assertEquals("Todo", title);
  }

  @Test
  void sessionFactoryIsAnEntityManagerFactory() {
    assertInstanceOf(EntityManagerFactory.class, sessionFactory);
    try (Session session = sessionFactory.openSession()) {
      assertInstanceOf(EntityManager.class, session);
    }
  }

  @Test
  void persistWithoutTransactionSavesNothing() {
    try (EntityManager em = sessionFactory.createEntityManager()) {
      em.persist(new Note("Lost", "Never saved", OCT_3));
    }
    assertEquals(2, count(sessionFactory));
  }

  @Test
  void flushWithoutTransactionFails() {
    try (EntityManager em = sessionFactory.createEntityManager()) {
      em.persist(new Note("Lost", "Never saved", OCT_3));
      Exception e = assertThrows(jakarta.persistence.TransactionRequiredException.class, em::flush);
      assertEquals("No active transaction", e.getMessage());
    }
  }

  @Test
  void unknownEntityFails() {
    try (EntityManagerFactory empty = new HibernatePersistenceConfiguration("empty")
        .jdbcUrl("jdbc:h2:mem:empty")
        .jdbcCredentials("sa", "")
        .createEntityManagerFactory()) {
      Exception e = assertThrows(IllegalArgumentException.class,
          () -> empty.runInTransaction(em -> em.persist(new Note("Groceries", "Milk", OCT_3))));
      assertEquals("Unknown entity type 'com.howtodoinjava.hibernate.helloworld.Note'"
          + " ('Note' does not belong to this persistence unit)", e.getMessage());
    }
  }

  @Test
  void javaConfigurationOverridesHibernateProperties() {
    assertEquals("jdbc:h2:mem:notes", jdbcUrl(sessionFactory));
  }

  @Test
  void hibernatePropertiesSuppliesMissingSettings() {
    try (EntityManagerFactory emf = new HibernatePersistenceConfiguration("notes-props")
        .managedClass(Note.class)
        .schemaToolingAction(Action.CREATE_DROP)
        .createEntityManagerFactory()) {
      assertEquals("jdbc:h2:mem:notes-props", jdbcUrl(emf));
      emf.runInTransaction(em -> em.persist(new Note("Groceries", "Milk", OCT_3)));
      assertEquals(1, count(emf));
    }
  }

  @Test
  void persistenceXmlBootstrapWorks() {
    try (EntityManagerFactory emf = Database.fromPersistenceXml()) {
      assertEquals("jdbc:h2:mem:notes-xml", jdbcUrl(emf));
      emf.runInTransaction(em -> em.persist(new Note("Groceries", "Milk", OCT_3)));
      assertEquals(1, count(emf));
    }
  }

  @Test
  void hibernateCfgXmlBootstrapStillWorks() {
    try (SessionFactory legacy = Database.fromHibernateCfgXml()) {
      assertEquals("jdbc:h2:mem:notes-cfg", jdbcUrl(legacy));
      legacy.inTransaction(session -> session.persist(new Note("Groceries", "Milk", OCT_3)));
      assertEquals(1, count(legacy));
    }
  }

  private static EntityManagerFactory factory(String url, Action action) {
    return new HibernatePersistenceConfiguration("schema")
        .managedClass(Note.class)
        .jdbcUrl(url)
        .jdbcCredentials("sa", "")
        .schemaToolingAction(action)
        .createEntityManagerFactory();
  }

  @Test
  void createKeepsTheTableAfterClose() {
    String url = "jdbc:h2:mem:keep;DB_CLOSE_DELAY=-1";
    try (EntityManagerFactory first = factory(url, Action.CREATE)) {
      first.runInTransaction(em -> em.persist(new Note("Groceries", "Milk", OCT_3)));
    }
    try (EntityManagerFactory second = factory(url, Action.NONE)) {
      assertEquals(1, count(second));
    }
  }

  @Test
  void createDropDropsTheTableOnClose() {
    String url = "jdbc:h2:mem:dropped;DB_CLOSE_DELAY=-1";
    try (EntityManagerFactory first = factory(url, Action.CREATE_DROP)) {
      first.runInTransaction(em -> em.persist(new Note("Groceries", "Milk", OCT_3)));
    }
    try (EntityManagerFactory second = factory(url, Action.NONE)) {
      assertThrows(RuntimeException.class, () -> count(second));
    }
  }

  @Test
  void quickReferenceSnippet() {
    try (SessionFactory sf = new HibernatePersistenceConfiguration("notes")
        .managedClass(Note.class)
        .jdbcUrl("jdbc:h2:mem:quick")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(true, false, false)
        .createEntityManagerFactory()) {
      Note groceries = new Note("Groceries", "Milk, eggs, bread", OCT_3);
      sf.runInTransaction(em -> em.persist(groceries));
      Long noteId = groceries.getId();
      assertEquals(1L, noteId);
      Note note = sf.callInTransaction(em -> em.find(Note.class, noteId));
      assertEquals("Groceries", note.getTitle());
      sf.runInTransaction(em -> em.find(Note.class, noteId).setText("Milk, eggs, bread, coffee"));
      assertEquals("Milk, eggs, bread, coffee", sf.callInTransaction(em -> em.find(Note.class, noteId)).getText());
      sf.runInTransaction(em -> em.remove(em.find(Note.class, noteId)));
      assertNull(sf.callInTransaction(em -> em.find(Note.class, noteId)));
    }
  }

  @Test
  void exceptionInLambdaRollsBack() {
    assertThrows(IllegalStateException.class, () -> sessionFactory.runInTransaction(em -> {
      em.persist(new Note("Lost", "Rolled back", OCT_3));
      throw new IllegalStateException("boom");
    }));
    assertEquals(2, count(sessionFactory));
  }

  @Test
  void removeRejectsADetachedEntity() {
    Note detached = sessionFactory.callInTransaction(em -> em.find(Note.class, id));
    assertThrows(IllegalArgumentException.class,
        () -> sessionFactory.runInTransaction(em -> em.remove(detached)));
    assertEquals(2, count(sessionFactory));
  }

  @Test
  void updateKeepsExistingRows() {
    String url = "jdbc:h2:mem:updated;DB_CLOSE_DELAY=-1";
    try (EntityManagerFactory first = factory(url, Action.CREATE)) {
      first.runInTransaction(em -> em.persist(new Note("Groceries", "Milk", OCT_3)));
    }
    try (EntityManagerFactory second = factory(url, Action.UPDATE)) {
      assertEquals(1, count(second));
    }
  }

  @Test
  void validateFailsWhenTheTableIsMissing() {
    Exception e = assertThrows(RuntimeException.class,
        () -> factory("jdbc:h2:mem:missing", Action.VALIDATE).close());
    assertEquals("Schema validation: missing table [Note]", e.getCause().getMessage());
  }

  @Test
  void sessionSaveNoLongerExists() throws NoSuchMethodException {
    assertThrows(NoSuchMethodException.class, () -> Session.class.getMethod("save", Object.class));
    assertThrows(NoSuchMethodException.class, () -> Session.class.getMethod("update", Object.class));
    assertThrows(NoSuchMethodException.class, () -> Session.class.getMethod("saveOrUpdate", Object.class));
    assertThrows(NoSuchMethodException.class, () -> Session.class.getMethod("load", Class.class, Object.class));
    assertNotNull(Session.class.getMethod("persist", Object.class));
    assertNotNull(Session.class.getMethod("getReference", Class.class, Object.class));
    assertNotNull(Session.class.getMethod("merge", Object.class));
  }
}
