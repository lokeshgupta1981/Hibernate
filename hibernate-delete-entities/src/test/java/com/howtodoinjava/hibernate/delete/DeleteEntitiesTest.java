package com.howtodoinjava.hibernate.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.RollbackException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteEntitiesTest {

  private EntityManagerFactory emf;
  private Long newsletterId;
  private Long alexId;
  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private PrintStream originalOut;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    newsletterId = Database.seed(emf);
    alexId = Database.subscriberId(emf, "Alex");
    Database.resetStatistics(emf);
    originalOut = System.out;
    System.setOut(new PrintStream(out, true));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
    emf.close();
  }

  private long count(String entity) {
    return Database.count(emf, entity);
  }

  private String printed() {
    return out.toString();
  }

  @Test
  void findThenRemoveDeletesTheRowAtCommit() {
    emf.runInTransaction(em -> {
      Subscriber alex = em.find(Subscriber.class, alexId);
      em.remove(alex);
      assertFalse(em.contains(alex));
      assertNull(em.find(Subscriber.class, alexId));
      assertEquals(1, Database.statements(emf));          // only the SELECT so far
    });
    assertEquals(2, Database.statements(emf));            // DELETE sent at commit
    assertEquals(2, count("Subscriber"));
    assertTrue(printed().contains("@PreRemove Alex"));
  }

  @Test
  void removedObjectKeepsItsIdAfterCommit() {
    Subscriber alex = emf.callInTransaction(em -> {
      Subscriber s = em.find(Subscriber.class, alexId);
      em.remove(s);
      return s;
    });
    assertEquals(alexId, alex.getId());
    assertEquals("Alex", alex.getName());
  }

  @Test
  void persistAfterRemoveCancelsTheDelete() {
    emf.runInTransaction(em -> {
      Subscriber alex = em.find(Subscriber.class, alexId);
      em.remove(alex);
      em.persist(alex);
      assertTrue(em.contains(alex));
    });
    assertEquals(1, Database.statements(emf));            // only the SELECT, no DELETE
    assertEquals(3, count("Subscriber"));
  }

  @Test
  void getReferenceThenRemoveLoadsSubscriberBecauseOfCallback() {
    emf.runInTransaction(em -> em.remove(em.getReference(Subscriber.class, alexId)));
    assertEquals(2, Database.statements(emf));            // SELECT + DELETE
    assertEquals(2, count("Subscriber"));
    assertTrue(printed().contains("@PreRemove Alex"));
  }

  @Test
  void getReferenceThenRemoveSendsOnlyDeleteForNewsletter() {
    Long kotlinId = emf.callInTransaction(em -> {
      Newsletter kotlin = new Newsletter("Kotlin Weekly");
      em.persist(kotlin);
      return kotlin.getId();
    });
    Database.resetStatistics(emf);
    emf.runInTransaction(em -> em.remove(em.getReference(Newsletter.class, kotlinId)));
    assertEquals(1, Database.statements(emf));            // DELETE only
    assertEquals(1, count("Newsletter"));
  }

  @Test
  void getReferenceOfMissingRowThrowsOnRemove() {
    EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
        () -> emf.runInTransaction(em -> em.remove(em.getReference(Subscriber.class, 99L))));
    assertTrue(e.getMessage().startsWith("No row with the given identifier exists"));
  }

  @Test
  void removeNullThrows() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> emf.runInTransaction(em -> em.remove(em.find(Subscriber.class, 99L))));
    assertEquals("Entity may not be null", e.getMessage());
  }

  @Test
  void removingDetachedEntityThrows() {
    Subscriber detached = emf.callInTransaction(em -> em.find(Subscriber.class, alexId));
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> emf.runInTransaction(em -> em.remove(detached)));
    assertEquals("org.hibernate.DetachedObjectException: "
        + "Given entity is not associated with the persistence context", e.getMessage());
    assertEquals(3, count("Subscriber"));
  }

  @Test
  void removingMergedCopyOfDetachedEntityWorks() {
    Subscriber detached = emf.callInTransaction(em -> em.find(Subscriber.class, alexId));
    emf.runInTransaction(em -> em.remove(em.merge(detached)));
    assertEquals(2, count("Subscriber"));
  }

  @Test
  void removingReloadedDetachedEntityWorks() {
    Subscriber detached = emf.callInTransaction(em -> em.find(Subscriber.class, alexId));
    emf.runInTransaction(em -> em.remove(em.getReference(Subscriber.class, detached.getId())));
    assertEquals(2, count("Subscriber"));
  }

  @Test
  void sessionRemoveWorksLikeEntityManagerRemove() {
    emf.runInTransaction(em -> {
      Session session = em.unwrap(Session.class);
      session.remove(session.find(Subscriber.class, alexId));
    });
    assertEquals(2, count("Subscriber"));
  }


  @Test
  void sessionRemoveOfDetachedEntityAlsoThrows() {
    Subscriber detached = emf.callInTransaction(em -> em.find(Subscriber.class, alexId));
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> emf.runInTransaction(em -> em.unwrap(Session.class).remove(detached)));
    assertTrue(e.getMessage().endsWith("Given entity is not associated with the persistence context"));
  }

  @Test
  void sessionHasNoDeleteMethodInHibernate7() {
    assertTrue(Arrays.stream(Session.class.getMethods())
        .noneMatch(m -> m.getName().equals("delete")));
  }

  @Test
  void bulkDeleteRunsOneStatementAndSkipsCallbacks() {
    int deleted = emf.callInTransaction(em -> em
        .createQuery("delete from Subscriber s where s.confirmed = false")
        .executeUpdate());
    assertEquals(2, deleted);
    assertEquals(1, Database.statements(emf));
    assertEquals(1, count("Subscriber"));
    assertFalse(printed().contains("@PreRemove"));
  }

  @Test
  void removeInLoopRunsOneStatementPerRowPlusSelect() {
    emf.runInTransaction(em -> em
        .createQuery("from Subscriber s where s.confirmed = false", Subscriber.class)
        .getResultList()
        .forEach(em::remove));
    assertEquals(3, Database.statements(emf));            // 1 SELECT + 2 DELETE
    assertTrue(printed().contains("@PreRemove Alex"));
    assertTrue(printed().contains("@PreRemove Brian"));
  }

  @Test
  void deleteByIdWithJpqlReturnsZeroForMissingRow() {
    int deleted = emf.callInTransaction(em -> em
        .createQuery("delete from Subscriber s where s.id = :id")
        .setParameter("id", 99L)
        .executeUpdate());
    assertEquals(0, deleted);
  }

  @Test
  void bulkDeleteLeavesLoadedEntityInPersistenceContext() {
    emf.runInTransaction(em -> {
      Subscriber alex = em.find(Subscriber.class, alexId);
      em.createQuery("delete from Subscriber s where s.confirmed = false").executeUpdate();
      assertTrue(em.contains(alex));
      assertSame(alex, em.find(Subscriber.class, alexId));
      em.clear();
      assertNull(em.find(Subscriber.class, alexId));
    });
  }

  @Test
  void bulkDeleteLeavesLoadedCollectionStale() {
    emf.runInTransaction(em -> {
      Newsletter javaWeekly = em.find(Newsletter.class, newsletterId);
      assertEquals(3, javaWeekly.getSubscribers().size());
      em.createQuery("delete from Subscriber s where s.confirmed = false").executeUpdate();
      assertEquals(3, javaWeekly.getSubscribers().size());
    });
  }

  @Test
  void removingStaleEntityAfterBulkDeleteFailsAtCommit() {
    RollbackException e = assertThrows(RollbackException.class, () ->
        emf.runInTransaction(em -> {
          Subscriber alex = em.find(Subscriber.class, alexId);
          em.createQuery("delete from Subscriber s where s.confirmed = false").executeUpdate();
          em.remove(alex);
        }));
    assertTrue(e.getMessage().contains("Unexpected row count (expected row count 1 but was 0)"));
    assertEquals(3, count("Subscriber"));                 // rolled back
  }

  @Test
  void bulkDeleteFlushesPendingInsertsFirst() {
    int deleted = emf.callInTransaction(em -> {
      Newsletter javaWeekly = em.find(Newsletter.class, newsletterId);
      Subscriber chris = new Subscriber("Chris", false);
      javaWeekly.addSubscriber(chris);
      em.persist(chris);
      return em.createQuery("delete from Subscriber s where s.confirmed = false").executeUpdate();
    });
    assertEquals(3, deleted);
  }

  @Test
  void removingNewsletterWithSubscribersFailsOnForeignKey() {
    RollbackException e = assertThrows(RollbackException.class,
        () -> emf.runInTransaction(em -> em.remove(em.find(Newsletter.class, newsletterId))));
    assertInstanceOf(ConstraintViolationException.class, e.getCause());
    assertTrue(e.getMessage().contains("Referential integrity constraint violation"));
    assertEquals(1, count("Newsletter"));
    assertEquals(3, count("Subscriber"));
  }

  @Test
  void bulkDeletingNewsletterWithSubscribersFailsOnForeignKey() {
    ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
        () -> emf.runInTransaction(em -> em.createQuery("delete from Newsletter").executeUpdate()));
    assertTrue(e.getMessage().contains("Referential integrity constraint violation"));
    assertEquals(1, count("Newsletter"));
  }

  @Test
  void deletingSubscribersFirstThenNewsletterWorks() {
    emf.runInTransaction(em -> {
      Newsletter javaWeekly = em.find(Newsletter.class, newsletterId);
      int deleted = em.createQuery("delete from Subscriber s where s.newsletter = :newsletter")
          .setParameter("newsletter", javaWeekly)
          .executeUpdate();
      assertEquals(3, deleted);
      em.remove(javaWeekly);
    });
    assertEquals(0, count("Newsletter"));
    assertEquals(0, count("Subscriber"));
  }

  @Test
  void removingEachSubscriberThenNewsletterWorks() {
    emf.runInTransaction(em -> {
      Newsletter javaWeekly = em.find(Newsletter.class, newsletterId);
      List<Subscriber> subscribers = List.copyOf(javaWeekly.getSubscribers());
      subscribers.forEach(em::remove);
      em.remove(javaWeekly);
    });
    assertEquals(0, count("Newsletter"));
    assertEquals(0, count("Subscriber"));
  }
}
