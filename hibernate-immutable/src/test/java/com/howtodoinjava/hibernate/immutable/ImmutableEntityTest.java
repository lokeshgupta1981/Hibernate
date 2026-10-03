package com.howtodoinjava.hibernate.immutable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.InstantiationException;
import org.hibernate.ReadOnlyMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImmutableEntityTest {

  private static final String UPDATE_ERROR = "The query attempts to update an immutable entity: [LedgerEntry] "
      + "(set 'hibernate.query.immutable_entity_update_query_handling_mode' to suppress)";

  private final SqlLog sql = new SqlLog();
  private EntityManagerFactory emf;
  private Long journalId;
  private Long coffeeId;

  @BeforeEach
  void setUp() {
    emf = Database.create(false, null, sql);
    journalId = Database.seed(emf);
    coffeeId = entryId("Coffee beans");
    sql.clear();
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private Long entryId(String description) {
    return emf.callInTransaction(em -> em
        .createQuery("select e.id from LedgerEntry e where e.description = :d", Long.class)
        .setParameter("d", description)
        .getSingleResult());
  }

  private long entryCount() {
    return emf.callInTransaction(em -> em
        .createQuery("select count(*) from LedgerEntry", Long.class).getSingleResult());
  }

  private Journal journal() {
    return emf.callInTransaction(em -> em.find(Journal.class, journalId));
  }

  private static EntityEntry entry(jakarta.persistence.EntityManager em, Object entity) {
    return em.unwrap(SessionImplementor.class).getPersistenceContextInternal()
        .getEntry(Hibernate.unproxy(entity));
  }

  // 1. Entity immutability

  @Test
  void changeToManagedImmutableEntityIsIgnoredWithoutWarning() {
    PrintStream err = System.err;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setErr(new PrintStream(captured));
    try {
      emf.runInTransaction(em -> em.find(LedgerEntry.class, coffeeId).setDescription("Tea"));
    } finally {
      System.setErr(err);
    }
    assertEquals(List.of(), sql.updates());
    assertEquals("", captured.toString());
    assertEquals("Coffee beans", Database.description(emf, coffeeId));
  }

  @Test
  void immutableEntityIsReadOnlyAndHasNoSnapshot() {
    emf.runInTransaction(em -> {
      LedgerEntry coffee = em.find(LedgerEntry.class, coffeeId);
      Journal journal = em.find(Journal.class, journalId);
      assertTrue(em.unwrap(Session.class).isReadOnly(coffee));
      assertEquals(Status.READ_ONLY, entry(em, coffee).getStatus());
      assertNull(entry(em, coffee).getLoadedState());
      assertFalse(em.unwrap(Session.class).isReadOnly(journal));
      assertEquals(Status.MANAGED, entry(em, journal).getStatus());
      assertNotNull(entry(em, journal).getLoadedState());
    });
  }

  @Test
  void persistInsertsImmutableEntity() {
    emf.runInTransaction(em -> {
      LedgerEntry milk = new LedgerEntry("Milk", "3.20", LocalDateTime.of(2026, 10, 3, 7, 0));
      milk.setJournal(em.find(Journal.class, journalId));
      em.persist(milk);
    });
    assertEquals(3, entryCount());
    assertTrue(sql.statements().stream().anyMatch(s -> s.startsWith("insert into LedgerEntry")));
  }

  @Test
  void removeDeletesImmutableEntity() {
    Long pancakesId = entryId("Pancakes");
    sql.clear();
    emf.runInTransaction(em -> em.remove(em.find(LedgerEntry.class, pancakesId)));
    assertTrue(sql.statements().contains("delete from LedgerEntry where id=?"));
    assertEquals(1, entryCount());
  }

  @Test
  void mergeOfChangedDetachedEntityIsIgnored() {
    LedgerEntry detached = emf.callInTransaction(em -> em.find(LedgerEntry.class, coffeeId));
    detached.setDescription("Green tea");
    sql.clear();
    LedgerEntry merged = emf.callInTransaction(em -> em.merge(detached));
    assertEquals("Green tea", merged.getDescription());
    assertEquals(List.of(), sql.updates());
    assertEquals("Coffee beans", Database.description(emf, coffeeId));
  }

  // 2. Bulk queries

  @Test
  void bulkUpdateThrowsByDefaultAtCreateQuery() {
    emf.runInTransaction(em -> {
      HibernateException e = assertThrows(HibernateException.class,
          () -> em.createQuery("update LedgerEntry set description = 'Tea' where id = :id"));
      assertEquals("org.hibernate.query.sqm.InterpretationException", e.getClass().getName());
      assertInstanceOf(HibernateException.class, e.getCause());
      assertEquals(UPDATE_ERROR, e.getCause().getMessage());
    });
    assertEquals("Coffee beans", Database.description(emf, coffeeId));
  }

  @Test
  void bulkUpdateModes() {
    emf.close();
    for (String mode : List.of("allow", "warning")) {
      try (EntityManagerFactory other = Database.create(false, mode, null)) {
        Database.seed(other);
        int rows = other.callInTransaction(em -> em
            .createQuery("update LedgerEntry set amount = amount * 2").executeUpdate());
        assertEquals(2, rows, mode);
      }
    }
    try (EntityManagerFactory other = Database.create(false, "exception", null)) {
      Database.seed(other);
      HibernateException e = assertThrows(HibernateException.class, () -> other.runInTransaction(em -> em
          .createQuery("update LedgerEntry set amount = amount * 2").executeUpdate()));
      assertEquals(UPDATE_ERROR, e.getCause().getMessage());
    }
    emf = Database.create(false, null, sql);
  }

  @Test
  void warningModeLogsHhh000487() {
    emf.close();
    PrintStream err = System.err;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    try (EntityManagerFactory other = Database.create(false, "warning", null)) {
      Database.seed(other);
      System.setErr(new PrintStream(captured));
      other.runInTransaction(em -> em.createQuery("update LedgerEntry set amount = amount * 2").executeUpdate());
    } finally {
      System.setErr(err);
    }
    assertTrue(captured.toString().contains("WARN org.hibernate.orm.core - HHH000487: The query "
        + "[update LedgerEntry set amount = amount * 2] updates an immutable entity: [LedgerEntry]"), captured.toString());
    emf = Database.create(false, null, sql);
  }

  @Test
  void allowModeLogsNothing() {
    emf.close();
    PrintStream err = System.err;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    try (EntityManagerFactory other = Database.create(false, "allow", null)) {
      Database.seed(other);
      System.setErr(new PrintStream(captured));
      other.runInTransaction(em -> em.createQuery("update LedgerEntry set amount = amount * 2").executeUpdate());
    } finally {
      System.setErr(err);
    }
    assertEquals("", captured.toString());
    emf = Database.create(false, null, sql);
  }

  @Test
  void bulkDeleteIsAllowedByDefault() {
    int rows = emf.callInTransaction(em -> em
        .createQuery("delete from LedgerEntry where description = 'Pancakes'").executeUpdate());
    assertEquals(1, rows);
    assertEquals(1, entryCount());
  }

  @Test
  void nativeUpdateIsNotChecked() {
    int rows = emf.callInTransaction(em -> em
        .createNativeQuery("update LedgerEntry set description = 'Tea' where id = ?1")
        .setParameter(1, coffeeId).executeUpdate());
    assertEquals(1, rows);
    assertEquals("Tea", Database.description(emf, coffeeId));
  }

  @Test
  void refreshReloadsRowChangedByNativeUpdate() {
    emf.runInTransaction(em -> {
      LedgerEntry coffee = em.find(LedgerEntry.class, coffeeId);
      em.createNativeQuery("update LedgerEntry set description = 'Tea' where id = ?1")
          .setParameter(1, coffeeId).executeUpdate();
      assertEquals("Coffee beans", em.find(LedgerEntry.class, coffeeId).getDescription());
      em.refresh(coffee);
      assertEquals("Tea", coffee.getDescription());
    });
  }

  @Test
  void bulkUpdateOfImmutableAttributeRuns() {
    int rows = emf.callInTransaction(em -> em
        .createQuery("update Journal set openedOn = :d").setParameter("d", LocalDate.of(2025, 1, 1)).executeUpdate());
    assertEquals(1, rows);
    assertEquals(LocalDate.of(2025, 1, 1), journal().getOpenedOn());
  }

  @Test
  void bulkUpdateOfNonUpdatableColumnRuns() {
    int rows = emf.callInTransaction(em -> em
        .createQuery("update Journal set owner = 'Alex'").executeUpdate());
    assertEquals(1, rows);
    assertEquals("Alex", journal().getOwner());
  }

  @Test
  void bulkUpdateRunsInReadOnlySession() {
    int rows = emf.callInTransaction(em -> {
      em.unwrap(Session.class).setDefaultReadOnly(true);
      return em.createQuery("update Journal set name = 'Home'").executeUpdate();
    });
    assertEquals(1, rows);
    assertEquals("Home", journal().getName());
  }

  @Test
  void bulkUpdateOfSubselectEntityThrows() {
    emf.runInTransaction(em -> {
      HibernateException e = assertThrows(HibernateException.class,
          () -> em.createQuery("update JournalBalance set name = 'Home'"));
      assertTrue(e.getCause().getMessage().startsWith("The query attempts to update an immutable entity"),
          e.getCause().getMessage());
    });
  }

  // 3. Immutable collection

  @Test
  void addingToImmutableCollectionThrowsAtCommit() {
    RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em ->
        em.find(Journal.class, journalId)
            .post(new LedgerEntry("Milk", "3.20", LocalDateTime.of(2026, 10, 3, 7, 0)))));
    assertEquals("Error while committing the transaction [Immutable collection was modified: "
        + "[com.howtodoinjava.hibernate.immutable.Journal.entries with owner id '" + journalId + "']]", e.getMessage());
    assertInstanceOf(HibernateException.class, e.getCause());
    assertEquals(2, entryCount());
  }

  @Test
  void removingFromImmutableCollectionThrowsAtCommit() {
    RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em ->
        em.find(Journal.class, journalId).getEntries().remove(0)));
    assertTrue(e.getMessage().contains("Immutable collection was modified"));
    assertEquals(2, entryCount());
  }

  // 4. Immutable attribute and converter, updatable = false

  @Test
  void immutableAttributeAndUpdatableFalseAreLeftOutOfUpdate() {
    emf.runInTransaction(em -> {
      Journal journal = em.find(Journal.class, journalId);
      journal.setName("Home");
      journal.setOpenedOn(LocalDate.of(2025, 1, 1));
      journal.setOwner("Alex");
    });
    assertEquals(List.of("update Journal set memo=?,name=? where id=?"), sql.updates());
    Journal journal = journal();
    assertEquals("Home", journal.getName());
    assertEquals(LocalDate.of(2026, 1, 1), journal.getOpenedOn());
    assertEquals("Lokesh", journal.getOwner());
  }

  @Test
  void changingOnlyNonUpdatableAttributesSendsNoUpdate() {
    emf.runInTransaction(em -> {
      Journal journal = em.find(Journal.class, journalId);
      journal.setOpenedOn(LocalDate.of(2024, 1, 1));
      journal.setOwner("Bob");
    });
    assertEquals(List.of(), sql.updates());
  }

  @Test
  void immutableConverterIgnoresInPlaceChange() {
    emf.runInTransaction(em -> em.find(Journal.class, journalId).setMemo(new Memo("Monthly")));
    sql.clear();
    emf.runInTransaction(em -> em.find(Journal.class, journalId).getMemo().setText("Weekly"));
    assertEquals(List.of(), sql.updates());
    assertEquals("Monthly", journal().getMemo().getText());

    emf.runInTransaction(em -> em.find(Journal.class, journalId).setMemo(new Memo("Weekly")));
    assertEquals(List.of("update Journal set memo=?,name=? where id=?"), sql.updates());
    assertEquals("Weekly", journal().getMemo().getText());
  }

  // 5. Read-only loading of a mutable entity

  @Test
  void readOnlyQueryHintSkipsDirtyChecking() {
    emf.runInTransaction(em -> {
      Journal journal = em.createQuery("from Journal", Journal.class)
          .setHint(HibernateHints.HINT_READ_ONLY, true)
          .getSingleResult();
      assertEquals(Status.READ_ONLY, entry(em, journal).getStatus());
      assertNull(entry(em, journal).getLoadedState());
      journal.setName("Ignored");
    });
    assertEquals(List.of(), sql.updates());
    assertEquals("Household", journal().getName());
  }

  @Test
  void readOnlyHintName() {
    assertEquals("org.hibernate.readOnly", HibernateHints.HINT_READ_ONLY);
  }

  @Test
  void selectionQuerySetReadOnlySkipsDirtyChecking() {
    emf.runInTransaction(em -> {
      Journal journal = em.unwrap(Session.class)
          .createSelectionQuery("from Journal", Journal.class)
          .setReadOnly(true)
          .getSingleResult();
      assertTrue(em.unwrap(Session.class).isReadOnly(journal));
      journal.setName("Ignored");
    });
    assertEquals(List.of(), sql.updates());
    assertEquals("Household", journal().getName());
  }

  @Test
  void findWithReadOnlyModeSkipsDirtyChecking() {
    emf.runInTransaction(em -> em.find(Journal.class, journalId, ReadOnlyMode.READ_ONLY).setName("Ignored"));
    assertEquals(List.of(), sql.updates());
  }

  @Test
  void sessionDefaultReadOnlySkipsDirtyChecking() {
    emf.runInTransaction(em -> {
      Session session = em.unwrap(Session.class);
      session.setDefaultReadOnly(true);
      session.find(Journal.class, journalId).setName("Ignored");
    });
    assertEquals(List.of(), sql.updates());
  }

  @Test
  void setReadOnlyFalseMakesEntityWritableAgain() {
    emf.runInTransaction(em -> {
      Session session = em.unwrap(Session.class);
      Journal journal = session.find(Journal.class, journalId, ReadOnlyMode.READ_ONLY);
      session.setReadOnly(journal, false);
      journal.setName("Family");
    });
    assertEquals(List.of("update Journal set memo=?,name=? where id=?"), sql.updates());
    assertEquals("Family", journal().getName());
  }

  @Test
  void readOnlyEntityCanStillBeRemoved() {
    emf.runInTransaction(em -> em.createQuery("delete from LedgerEntry").executeUpdate());
    sql.clear();
    emf.runInTransaction(em -> {
      Journal journal = em.find(Journal.class, journalId, ReadOnlyMode.READ_ONLY);
      assertTrue(em.unwrap(Session.class).isReadOnly(journal));
      em.remove(journal);
    });
    assertTrue(sql.statements().contains("delete from Journal where id=?"), sql.statements().toString());
    assertNull(journal());
  }

  // 6. @Subselect view entity

  @Test
  void subselectEntityReadsBalance() {
    List<JournalBalance> balances = emf.callInTransaction(em -> em
        .createQuery("from JournalBalance", JournalBalance.class).getResultList());
    assertEquals(1, balances.size());
    assertEquals("Household", balances.get(0).getName());
    assertEquals(new BigDecimal("52.50"), balances.get(0).getBalance());
  }

  @Test
  void synchronizeFlushesPendingInsertBeforeSubselectQuery() {
    BigDecimal balance = emf.callInTransaction(em -> {
      LedgerEntry milk = new LedgerEntry("Milk", "3.20", LocalDateTime.of(2026, 10, 3, 7, 0));
      milk.setJournal(em.find(Journal.class, journalId));
      em.persist(milk);
      return em.createQuery("from JournalBalance", JournalBalance.class).getSingleResult().getBalance();
    });
    assertEquals(new BigDecimal("55.70"), balance);
  }

  // 7. Java records

  @Test
  void recordProjectionReturnsReadOnlyValues() {
    List<EntryLine> lines = emf.callInTransaction(em -> em
        .createQuery("select e.description, e.amount from LedgerEntry e order by e.postedAt", EntryLine.class)
        .getResultList());
    assertEquals(List.of(new EntryLine("Coffee beans", new BigDecimal("40.00")),
        new EntryLine("Pancakes", new BigDecimal("12.50"))), lines);
  }

  @Test
  void recordCannotBeLoadedAsEntity() {
    try (EntityManagerFactory records = new HibernatePersistenceConfiguration("records")
        .managedClasses(LedgerEntryRecord.class)
        .jdbcUrl("jdbc:h2:mem:records")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .createEntityManagerFactory()) {
      records.runInTransaction(em -> em.persist(new LedgerEntryRecord(1L, "Coffee beans")));
      InstantiationException e = assertThrows(InstantiationException.class,
          () -> records.callInTransaction(em -> em.find(LedgerEntryRecord.class, 1L)));
      assertEquals("No default constructor for entity "
          + "'com.howtodoinjava.hibernate.immutable.LedgerEntryRecord'", e.getMessage());
    }
  }
}
