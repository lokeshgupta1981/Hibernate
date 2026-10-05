package com.howtodoinjava.hibernate.mergerefresh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MergeRefreshTest {

  private EntityManagerFactory emf;
  private Shipment pune;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    pune = new Shipment("Pune", "packed");
    emf.runInTransaction(em -> em.persist(pune));
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  @Test
  void objectIsDetachedAfterTransaction() {
    assertEquals(1L, pune.getId());
    emf.runInTransaction(em -> assertFalse(em.contains(pune)));
  }

  @Test
  void changingDetachedObjectSavesNothing() {
    pune.setStatus("shipped");
    assertEquals("packed", Database.load(emf, pune.getId()).getStatus());
  }

  @Test
  void mergeReturnsManagedCopyAndUpdatesRow() {
    pune.setStatus("shipped");
    Statistics stats = Database.statistics(emf);
    Shipment copy = emf.callInTransaction(em -> {
      Shipment merged = em.merge(pune);
      assertEquals(1, stats.getPrepareStatementCount());   // the select
      assertNotSame(pune, merged);
      assertTrue(em.contains(merged));
      assertFalse(em.contains(pune));
      return merged;
    });
    assertEquals(2, stats.getPrepareStatementCount());     // select + update
    assertEquals(1, stats.getEntityUpdateCount());
    assertEquals("shipped", copy.getStatus());
    assertEquals(1, copy.getVersion());
    assertEquals(0, pune.getVersion());
    assertEquals("shipped", Database.load(emf, pune.getId()).getStatus());
  }

  @Test
  void changesToArgumentAfterMergeAreIgnored() {
    emf.runInTransaction(em -> {
      Shipment copy = em.merge(pune);
      pune.setDestination("Mumbai");
      copy.setStatus("shipped");
      assertNotSame(pune, copy);
    });
    Shipment row = Database.load(emf, pune.getId());
    assertEquals("Pune", row.getDestination());
    assertEquals("shipped", row.getStatus());
  }

  @Test
  void changesToReturnedCopyAreSaved() {
    emf.runInTransaction(em -> {
      Shipment copy = em.merge(pune);
      copy.setStatus("shipped");
    });
    assertEquals("shipped", Database.load(emf, pune.getId()).getStatus());
  }

  @Test
  void mergeCopiesOntoAlreadyLoadedInstanceWithoutSelect() {
    Shipment detached = Database.load(emf, pune.getId());
    detached.setStatus("in transit");
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Shipment loaded = em.find(Shipment.class, pune.getId());
      Shipment copy = em.merge(detached);
      assertEquals(1, stats.getPrepareStatementCount());   // only the find
      assertSame(loaded, copy);
      assertEquals("in transit", loaded.getStatus());
    });
    assertEquals("in transit", Database.load(emf, pune.getId()).getStatus());
  }

  @Test
  void mergeOfManagedEntityReturnsSameInstance() {
    emf.runInTransaction(em -> {
      Shipment loaded = em.find(Shipment.class, pune.getId());
      Statistics stats = Database.statistics(emf);
      assertSame(loaded, em.merge(loaded));
      assertEquals(0, stats.getPrepareStatementCount());
    });
  }

  @Test
  void mergeNewObjectInsertsCopy() {
    Shipment delhi = new Shipment("Delhi", "packed");
    Statistics stats = Database.statistics(emf);
    Shipment saved = emf.callInTransaction(em -> em.merge(delhi));
    assertEquals(1, stats.getEntityInsertCount());
    assertEquals(1, stats.getPrepareStatementCount());
    assertNull(delhi.getId());
    assertEquals(2L, saved.getId());
    assertEquals(0, saved.getVersion());
  }

  @Test
  void mergeOutdatedCopyThrowsOptimisticLockException() {
    pune.setStatus("shipped");
    emf.runInTransaction(em -> em.merge(pune));            // row version is now 1, pune still 0
    pune.setStatus("lost");
    emf.runInTransaction(em -> {
      OptimisticLockException e = assertThrows(OptimisticLockException.class, () -> em.merge(pune));
      assertEquals("Row was already updated or deleted by another transaction for entity "
          + "[com.howtodoinjava.hibernate.mergerefresh.Shipment with id '1']", e.getMessage());
      em.getTransaction().setRollbackOnly();
    });
    assertEquals("shipped", Database.load(emf, pune.getId()).getStatus());
  }

  @Test
  void continuingWithReturnedCopyAvoidsOptimisticLockException() {
    Shipment copy = emf.callInTransaction(em -> {
      pune.setStatus("shipped");
      return em.merge(pune);
    });
    copy.setStatus("delivered");
    Shipment again = emf.callInTransaction(em -> em.merge(copy));
    assertEquals(2, again.getVersion());
    assertEquals("delivered", Database.load(emf, pune.getId()).getStatus());
  }

  @Test
  void persistDetachedObjectThrows() {
    emf.runInTransaction(em -> {
      EntityExistsException e = assertThrows(EntityExistsException.class, () -> em.persist(pune));
      assertEquals("Detached entity passed to persist: com.howtodoinjava.hibernate.mergerefresh.Shipment",
          e.getMessage());
      em.getTransaction().setRollbackOnly();
    });
  }

  @Test
  void refreshDiscardsUnsavedChanges() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Shipment shipment = em.find(Shipment.class, pune.getId());
      shipment.setStatus("lost");
      em.refresh(shipment);
      assertEquals("packed", shipment.getStatus());
    });
    assertEquals(2, stats.getPrepareStatementCount());     // find + refresh, no update
    assertEquals(0, stats.getEntityUpdateCount());
    assertEquals("packed", Database.load(emf, pune.getId()).getStatus());
  }

  @Test
  void refreshAfterBulkUpdate() {
    emf.runInTransaction(em -> {
      Shipment shipment = em.find(Shipment.class, pune.getId());
      int rows = em.createQuery("update Shipment set status = 'delivered' where destination = 'Pune'")
          .executeUpdate();
      assertEquals(1, rows);
      assertEquals("packed", em.find(Shipment.class, pune.getId()).getStatus());
      assertEquals("packed", em.createQuery("from Shipment where id = :id", Shipment.class)
          .setParameter("id", pune.getId()).getSingleResult().getStatus());
      em.refresh(shipment);
      assertEquals("delivered", shipment.getStatus());
      assertEquals(0, shipment.getVersion());              // bulk update does not change the version
    });
  }

  @Test
  void refreshAfterNativeUpdate() {
    emf.runInTransaction(em -> {
      Shipment shipment = em.find(Shipment.class, pune.getId());
      em.createNativeQuery("update Shipment set status = 'shipped' where id = ?")
          .setParameter(1, pune.getId())
          .executeUpdate();
      assertEquals("packed", shipment.getStatus());
      em.refresh(shipment);
      assertEquals("shipped", shipment.getStatus());
    });
  }

  @Test
  void refreshDetachedObjectThrows() {
    emf.runInTransaction(em -> {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> em.refresh(pune));
      assertEquals("org.hibernate.DetachedObjectException: Given entity is not associated with the persistence context",
          e.getMessage());
      em.getTransaction().setRollbackOnly();
    });
  }

  @Test
  void refreshNewObjectThrows() {
    emf.runInTransaction(em -> {
      assertThrows(IllegalArgumentException.class, () -> em.refresh(new Shipment("Goa", "packed")));
      em.getTransaction().setRollbackOnly();
    });
  }

  @Test
  void refreshReadsValueSetByTrigger() {
    emf.runInTransaction(em -> {
      Shipment chennai = new Shipment("Chennai", null);
      em.persist(chennai);
      em.flush();
      assertNull(chennai.getStatus());
      em.refresh(chennai);
      assertEquals("received", chennai.getStatus());
    });
  }

  @Test
  void sessionMergeReplacesUpdate() {
    pune.setStatus("delivered");
    emf.runInTransaction(em -> em.unwrap(Session.class).merge(pune));
    assertEquals("delivered", Database.load(emf, pune.getId()).getStatus());
  }

  @Test
  void sessionUpdateAndSaveOrUpdateNoLongerExist() {
    Method[] methods = Session.class.getMethods();
    assertFalse(Arrays.stream(methods).anyMatch(m -> m.getName().equals("update")));
    assertFalse(Arrays.stream(methods).anyMatch(m -> m.getName().equals("saveOrUpdate")));
    assertFalse(Arrays.stream(methods).anyMatch(m -> m.getName().equals("save")));
  }

  @Test
  void mergeHalfFilledObjectWritesNulls() {
    Shipment form = new Shipment(null, "returned");
    form.setId(pune.getId());
    emf.runInTransaction(em -> em.merge(form));
    Shipment row = Database.load(emf, pune.getId());
    assertNull(row.getDestination());
    assertEquals("returned", row.getStatus());
  }

  @Test
  void findThenSetChangesOnlyOneField() {
    emf.runInTransaction(em -> em.find(Shipment.class, pune.getId()).setStatus("returned"));
    Shipment row = Database.load(emf, pune.getId());
    assertEquals("Pune", row.getDestination());
    assertEquals("returned", row.getStatus());
  }

  @Test
  void refreshWithLockAddsForUpdate() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Shipment shipment = em.find(Shipment.class, pune.getId());
      em.refresh(shipment, LockModeType.PESSIMISTIC_WRITE);
      assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(shipment));
    });
    assertEquals(2, stats.getPrepareStatementCount());
  }

  @Test
  void mergeDetachedObjectWhoseRowWasDeletedThrows() {
    emf.runInTransaction(em -> em.remove(em.find(Shipment.class, pune.getId())));
    emf.runInTransaction(em -> {
      OptimisticLockException e = assertThrows(OptimisticLockException.class, () -> em.merge(pune));
      assertEquals("Row was already updated or deleted by another transaction for entity "
          + "[com.howtodoinjava.hibernate.mergerefresh.Shipment with id '1']", e.getMessage());
      em.getTransaction().setRollbackOnly();
    });
  }

  @Test
  void quickReference() {
    pune.setStatus("shipped");
    emf.runInTransaction(em -> {
      Shipment copy = em.merge(pune);
      assertNotSame(pune, copy);
    });
    emf.runInTransaction(em -> {
      Shipment shipment = em.find(Shipment.class, pune.getId());
      assertEquals("shipped", shipment.getStatus());
      shipment.setStatus("lost");
      em.refresh(shipment);
      assertEquals("shipped", shipment.getStatus());
    });
    assertEquals("shipped", Database.load(emf, pune.getId()).getStatus());
  }

  @Test
  void refreshDoesNotFlushPendingChanges() {
    pune.setStatus("shipped");
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Shipment copy = em.merge(pune);
      em.refresh(copy);
      assertEquals("packed", copy.getStatus());
    });
    assertEquals(0, stats.getEntityUpdateCount());
    assertEquals("packed", Database.load(emf, pune.getId()).getStatus());
  }

  @Test
  void flushBeforeRefreshKeepsMergedChanges() {
    pune.setStatus("shipped");
    emf.runInTransaction(em -> {
      Shipment copy = em.merge(pune);
      em.flush();
      em.refresh(copy);
      assertEquals("shipped", copy.getStatus());
      assertEquals(1, copy.getVersion());
    });
  }

  @Test
  void clearAndDetachMakeEntitiesDetached() {
    emf.runInTransaction(em -> {
      Shipment shipment = em.find(Shipment.class, pune.getId());
      em.detach(shipment);
      assertFalse(em.contains(shipment));
      Shipment again = em.find(Shipment.class, pune.getId());
      em.clear();
      assertFalse(em.contains(again));
    });
  }

  @Test
  void bulkUpdateCanIncreaseVersionExplicitly() {
    emf.runInTransaction(em -> {
      Shipment shipment = em.find(Shipment.class, pune.getId());
      em.createQuery("update Shipment set status = 'delivered', version = version + 1 where destination = 'Pune'")
          .executeUpdate();
      em.refresh(shipment);
      assertEquals(1, shipment.getVersion());
    });
  }
}
