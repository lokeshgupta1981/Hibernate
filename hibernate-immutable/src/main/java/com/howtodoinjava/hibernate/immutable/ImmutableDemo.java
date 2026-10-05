package com.howtodoinjava.hibernate.immutable;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.ReadOnlyMode;
import org.hibernate.Session;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jpa.HibernateHints;

public class ImmutableDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Persist a journal with two ledger entries");
      Long journalId = Database.seed(emf);
      Long coffeeId = id(emf, "Coffee beans");
      Long pancakesId = id(emf, "Pancakes");

      step("2. Change a managed LedgerEntry (@Immutable entity)");
      emf.runInTransaction(em -> {
        LedgerEntry coffee = em.find(LedgerEntry.class, coffeeId);
        Journal journal = em.find(Journal.class, journalId);
        SessionImplementor session = em.unwrap(SessionImplementor.class);
        System.out.println("isReadOnly(coffee) = " + session.isReadOnly(coffee));
        System.out.println("LedgerEntry: " + describe(session, coffee));
        System.out.println("Journal:     " + describe(session, journal));
        coffee.setDescription("Tea");
        System.out.println("-- commit");
      });
      System.out.println("description in database = " + Database.description(emf, coffeeId));

      step("3. merge() a detached, changed LedgerEntry");
      LedgerEntry detached = emf.callInTransaction(em -> em.find(LedgerEntry.class, coffeeId));
      detached.setDescription("Green tea");
      emf.runInTransaction(em -> System.out.println("merged = " + em.merge(detached)));
      System.out.println("description in database = " + Database.description(emf, coffeeId));

      step("4. Record projection");
      List<EntryLine> lines = emf.callInTransaction(em -> em
          .createQuery("select e.description, e.amount from LedgerEntry e order by e.postedAt", EntryLine.class)
          .getResultList());
      System.out.println(lines);

      step("5. Add an entry to the @Immutable collection");
      try {
        emf.runInTransaction(em -> em.find(Journal.class, journalId)
            .post(new LedgerEntry("Milk", "3.20", LocalDateTime.of(2026, 10, 3, 7, 0))));
      } catch (RuntimeException e) {
        print(e);
      }

      step("6. Remove an entry from the @Immutable collection");
      try {
        emf.runInTransaction(em -> em.find(Journal.class, journalId).getEntries().remove(0));
      } catch (RuntimeException e) {
        print(e);
      }

      step("7. Persist a new entry without touching the collection, then read the @Subselect balance");
      emf.runInTransaction(em -> {
        LedgerEntry milk = new LedgerEntry("Milk", "3.20", LocalDateTime.of(2026, 10, 3, 7, 0));
        milk.setJournal(em.find(Journal.class, journalId));
        em.persist(milk);
        System.out.println(em.createQuery("from JournalBalance", JournalBalance.class).getResultList());
      });

      step("8. Change name, openedOn (@Immutable) and owner (updatable = false) of the Journal");
      emf.runInTransaction(em -> {
        Journal journal = em.find(Journal.class, journalId);
        journal.setName("Home");
        journal.setOpenedOn(LocalDate.of(2025, 1, 1));
        journal.setOwner("Alex");
      });
      emf.runInTransaction(em -> {
        Journal j = em.find(Journal.class, journalId);
        System.out.println("name = " + j.getName() + ", openedOn = " + j.getOpenedOn() + ", owner = " + j.getOwner());
      });

      step("9. Memo with an @Immutable converter: set, change in place, replace");
      emf.runInTransaction(em -> em.find(Journal.class, journalId).setMemo(new Memo("Monthly")));
      System.out.println("-- getMemo().setText(\"Weekly\")");
      emf.runInTransaction(em -> em.find(Journal.class, journalId).getMemo().setText("Weekly"));
      emf.runInTransaction(em -> System.out.println("memo = " + em.find(Journal.class, journalId).getMemo().getText()));
      System.out.println("-- setMemo(new Memo(\"Weekly\"))");
      emf.runInTransaction(em -> em.find(Journal.class, journalId).setMemo(new Memo("Weekly")));

      step("10. Read-only loading of the mutable Journal");
      System.out.println("-- query hint " + HibernateHints.HINT_READ_ONLY);
      emf.runInTransaction(em -> em.createQuery("from Journal", Journal.class)
          .setHint(HibernateHints.HINT_READ_ONLY, true)
          .getSingleResult()
          .setName("Ignored"));
      System.out.println("-- SelectionQuery.setReadOnly(true)");
      emf.runInTransaction(em -> em.unwrap(Session.class)
          .createSelectionQuery("from Journal", Journal.class)
          .setReadOnly(true)
          .getSingleResult()
          .setName("Ignored"));
      System.out.println("-- find(..., ReadOnlyMode.READ_ONLY)");
      emf.runInTransaction(em -> em.find(Journal.class, journalId, ReadOnlyMode.READ_ONLY).setName("Ignored"));
      System.out.println("-- session.setDefaultReadOnly(true)");
      emf.runInTransaction(em -> {
        Session session = em.unwrap(Session.class);
        session.setDefaultReadOnly(true);
        session.find(Journal.class, journalId).setName("Ignored");
      });
      System.out.println("-- setReadOnly(journal, false)");
      emf.runInTransaction(em -> {
        Session session = em.unwrap(Session.class);
        Journal journal = session.find(Journal.class, journalId, ReadOnlyMode.READ_ONLY);
        session.setReadOnly(journal, false);
        journal.setName("Family");
      });

      step("11. JPQL update on LedgerEntry (default mode)");
      try {
        emf.runInTransaction(em -> em
            .createQuery("update LedgerEntry set description = 'Tea' where id = :id")
            .setParameter("id", coffeeId)
            .executeUpdate());
      } catch (RuntimeException e) {
        print(e);
      }

      step("12. JPQL delete on LedgerEntry (default mode)");
      emf.runInTransaction(em -> System.out.println("rows = " + em
          .createQuery("delete from LedgerEntry where description = 'Milk'")
          .executeUpdate()));

      step("13. Native SQL update on LedgerEntry");
      emf.runInTransaction(em -> System.out.println("rows = " + em
          .createNativeQuery("update LedgerEntry set description = 'Tea' where id = ?1")
          .setParameter(1, coffeeId)
          .executeUpdate()));
      System.out.println("description in database = " + Database.description(emf, coffeeId));

      step("14. em.remove() a LedgerEntry");
      emf.runInTransaction(em -> em.remove(em.find(LedgerEntry.class, pancakesId)));
    }

    for (String mode : new String[] {"warning", "allow", "exception"}) {
      step("15. JPQL update with immutable_entity_update_query_handling_mode = " + mode);
      try (EntityManagerFactory emf = Database.create(true, mode, null)) {
        Database.seed(emf);
        emf.runInTransaction(em -> System.out.println("rows = " + em
            .createQuery("update LedgerEntry set amount = amount * 2")
            .executeUpdate()));
        System.err.flush();
      } catch (RuntimeException e) {
        print(e);
      }
    }
  }

  private static Long id(EntityManagerFactory emf, String description) {
    return emf.callInTransaction(em -> em
        .createQuery("select e.id from LedgerEntry e where e.description = :d", Long.class)
        .setParameter("d", description)
        .getSingleResult());
  }

  // Prints the entity status and the loaded-state snapshot Hibernate keeps for dirty checking
  private static String describe(SessionImplementor session, Object entity) {
    EntityEntry entry = session.getPersistenceContextInternal().getEntry(Hibernate.unproxy(entity));
    Object[] snapshot = entry.getLoadedState();
    String text = "null";
    if (snapshot != null) {
      text = Arrays.toString(Arrays.stream(snapshot)
          .map(v -> v instanceof PersistentCollection<?> ? "<entries>" : v)
          .toArray());
    }
    return "status = " + entry.getStatus() + ", snapshot = " + text;
  }

  private static void print(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      System.out.println(t.getClass().getName() + ": " + t.getMessage());
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
