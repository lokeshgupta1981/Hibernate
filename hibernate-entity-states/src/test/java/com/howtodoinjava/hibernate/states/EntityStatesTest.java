package com.howtodoinjava.hibernate.states;

import static com.howtodoinjava.hibernate.states.Database.stateOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.DetachedObjectException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntityStatesTest {

  private final List<String> sql = new ArrayList<>();
  private EntityManagerFactory emf;
  private ServiceOrder brakes;

  @BeforeEach
  void setUp() {
    emf = Database.create(false, sql);
    brakes = new ServiceOrder("1234", "Brake noise", "open", new BigDecimal("40.00"));
    emf.runInTransaction(em -> em.persist(brakes));
    sql.clear();
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private ServiceOrder newOrder() {
    return new ServiceOrder("5678", "Oil change", "open", new BigDecimal("25.00"));
  }

  private List<String> statements(String verb) {
    return sql.stream().filter(s -> s.startsWith(verb)).toList();
  }

  @Test
  void newObjectIsTransientWithoutId() {
    ServiceOrder order = newOrder();
    emf.runInTransaction(em -> {
      assertFalse(em.contains(order));
      assertNull(emf.getPersistenceUnitUtil().getIdentifier(order));
      assertEquals(EntityState.TRANSIENT, stateOf(em, order));
    });
    assertTrue(sql.isEmpty());
  }

  @Test
  void persistMakesManagedAndAssignsIdButInsertsAtCommit() {
    ServiceOrder order = newOrder();
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    em.persist(order);
    assertTrue(em.contains(order));
    assertNotNull(order.getId());
    assertEquals(EntityState.MANAGED, stateOf(em, order));
    assertTrue(statements("insert").isEmpty());
    em.getTransaction().commit();
    assertEquals(List.of("insert into ServiceOrder (cost,issue,plateNumber,status,id) values (?,?,?,?,?)"),
        statements("insert"));
    em.close();
  }

  @Test
  void findAndQueryReturnManagedEntities() {
    emf.runInTransaction(em -> {
      ServiceOrder found = em.find(ServiceOrder.class, brakes.getId());
      assertEquals(EntityState.MANAGED, stateOf(em, found));
      ServiceOrder queried = em.createQuery("from ServiceOrder where status = :status", ServiceOrder.class)
          .setParameter("status", "open").getSingleResult();
      assertSame(found, queried);
    });
  }

  @Test
  void dirtyCheckingUpdatesManagedEntityWithoutAnyCall() {
    emf.runInTransaction(em -> {
      ServiceOrder order = em.find(ServiceOrder.class, brakes.getId());
      assertFalse(em.unwrap(Session.class).isDirty());
      order.setStatus("in progress");
      assertTrue(em.unwrap(Session.class).isDirty());
    });
    assertEquals(List.of("update ServiceOrder set cost=?,issue=?,plateNumber=?,status=? where id=?"),
        statements("update"));
    assertEquals("in progress", Database.load(emf, brakes.getId()).getStatus());
  }

  @Test
  void unchangedManagedEntitySendsNoUpdate() {
    emf.runInTransaction(em -> em.find(ServiceOrder.class, brakes.getId()));
    assertTrue(statements("update").isEmpty());
  }

  @Test
  void flushSendsUpdateBeforeCommit() {
    emf.runInTransaction(em -> {
      em.find(ServiceOrder.class, brakes.getId()).setStatus("done");
      em.flush();
      assertEquals(1, statements("update").size());
      assertFalse(em.unwrap(Session.class).isDirty());
    });
    assertEquals(1, statements("update").size());
  }

  @Test
  void queryOnTheTableFlushesPendingChangesFirst() {
    emf.runInTransaction(em -> {
      em.find(ServiceOrder.class, brakes.getId()).setStatus("in progress");
      sql.clear();
      long count = em.createQuery("select count(*) from ServiceOrder where status = 'in progress'", Long.class)
          .getSingleResult();
      assertEquals(1, count);
      assertTrue(sql.get(0).startsWith("update ServiceOrder"));
      assertTrue(sql.get(1).startsWith("select count(*)"));
    });
  }

  @Test
  void entityIsDetachedAfterTransactionEnds() {
    // runInTransaction closed its EntityManager, so brakes is detached
    emf.runInTransaction(em -> {
      assertFalse(em.contains(brakes));
      assertEquals(EntityState.DETACHED, stateOf(em, brakes));
    });
  }

  @Test
  void closeDetachesEntity() {
    EntityManager em = emf.createEntityManager();
    ServiceOrder order = em.find(ServiceOrder.class, brakes.getId());
    em.close();
    order.setCost(new BigDecimal("55.00"));
    emf.runInTransaction(other -> assertEquals(EntityState.DETACHED, stateOf(other, order)));
    assertEquals(new BigDecimal("40.00"), Database.load(emf, brakes.getId()).getCost());
  }

  @Test
  void detachIgnoresLaterChanges() {
    emf.runInTransaction(em -> {
      ServiceOrder order = em.find(ServiceOrder.class, brakes.getId());
      em.detach(order);
      assertEquals(EntityState.DETACHED, stateOf(em, order));
      order.setStatus("lost");
      ServiceOrder again = em.find(ServiceOrder.class, brakes.getId());
      assertNotSame(order, again);
      assertEquals("open", again.getStatus());
    });
    assertTrue(statements("update").isEmpty());
    assertEquals("open", Database.load(emf, brakes.getId()).getStatus());
  }

  @Test
  void clearDetachesEverything() {
    emf.runInTransaction(em -> {
      ServiceOrder order = em.find(ServiceOrder.class, brakes.getId());
      order.setStatus("lost");
      em.clear();
      assertEquals(EntityState.DETACHED, stateOf(em, order));
    });
    assertTrue(statements("update").isEmpty());
  }

  @Test
  void mergeReturnsNewManagedCopyAndSavesChanges() {
    brakes.setCost(new BigDecimal("55.00"));
    ServiceOrder copy = emf.callInTransaction(em -> {
      ServiceOrder managed = em.merge(brakes);
      assertNotSame(brakes, managed);
      assertEquals(EntityState.DETACHED, stateOf(em, brakes));
      assertEquals(EntityState.MANAGED, stateOf(em, managed));
      assertEquals(1, statements("select").size());
      return managed;
    });
    assertEquals(new BigDecimal("55.00"), copy.getCost());
    assertEquals(List.of("update ServiceOrder set cost=?,issue=?,plateNumber=?,status=? where id=?"),
        statements("update"));
    assertEquals(new BigDecimal("55.00"), Database.load(emf, brakes.getId()).getCost());
  }

  @Test
  void changeToArgumentAfterMergeIsNotSaved() {
    emf.runInTransaction(em -> {
      em.merge(brakes);
      brakes.setStatus("lost");
    });
    assertTrue(statements("update").isEmpty());
    assertEquals("open", Database.load(emf, brakes.getId()).getStatus());
  }

  @Test
  void sessionEvictDetachesLikeDetach() {
    emf.runInTransaction(em -> {
      ServiceOrder order = em.find(ServiceOrder.class, brakes.getId());
      em.unwrap(Session.class).evict(order);
      assertEquals(EntityState.DETACHED, stateOf(em, order));
      order.setStatus("lost");
    });
    assertTrue(statements("update").isEmpty());
  }

  @Test
  void legacySessionMethodsAreGoneInHibernate7() {
    List<String> names = Arrays.stream(Session.class.getMethods()).map(Method::getName).toList();
    assertFalse(names.contains("save"));
    assertFalse(names.contains("update"));
    assertFalse(names.contains("saveOrUpdate"));
    assertFalse(names.contains("delete"));
    assertTrue(Arrays.stream(Session.class.getMethods())
        .noneMatch(m -> m.getName().equals("load") && m.getParameterTypes()[0] == Class.class));
    assertTrue(names.contains("evict"));
  }

  @Test
  void mergeOfTransientObjectInsertsCopy() {
    ServiceOrder order = newOrder();
    emf.runInTransaction(em -> {
      ServiceOrder copy = em.merge(order);
      assertEquals(EntityState.TRANSIENT, stateOf(em, order));
      assertEquals(EntityState.MANAGED, stateOf(em, copy));
    });
    assertEquals(1, statements("insert").size());
    assertNull(order.getId());
  }

  @Test
  void removeMarksRemovedAndDeletesAtCommit() {
    emf.runInTransaction(em -> {
      ServiceOrder order = em.find(ServiceOrder.class, brakes.getId());
      em.remove(order);
      assertFalse(em.contains(order));
      assertEquals(EntityState.REMOVED, stateOf(em, order));
      assertTrue(statements("delete").isEmpty());
      assertEquals(brakes.getId(), order.getId());
    });
    assertEquals(List.of("delete from ServiceOrder where id=?"), statements("delete"));
    assertEquals(0, Database.count(emf));
  }

  @Test
  void removedEntityKeepsIdAfterCommitAndLooksDetachedElsewhere() {
    ServiceOrder removed = emf.callInTransaction(em -> {
      ServiceOrder order = em.find(ServiceOrder.class, brakes.getId());
      em.remove(order);
      return order;
    });
    assertEquals(brakes.getId(), removed.getId());
    assertEquals(1L, removed.getId());
    emf.runInTransaction(em -> assertEquals(EntityState.DETACHED, stateOf(em, removed)));
  }

  @Test
  void persistAfterRemoveMakesManagedAgain() {
    emf.runInTransaction(em -> {
      ServiceOrder order = em.find(ServiceOrder.class, brakes.getId());
      em.remove(order);
      em.persist(order);
      assertEquals(EntityState.MANAGED, stateOf(em, order));
    });
    assertTrue(statements("delete").isEmpty());
    assertTrue(statements("insert").isEmpty());
    assertEquals(1, Database.count(emf));
  }

  @Test
  void removeOfDetachedEntityThrows() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> emf.runInTransaction(em -> em.remove(brakes)));
    assertEquals("org.hibernate.DetachedObjectException: Given entity is not associated with the persistence context",
        e.getMessage());
    assertInstanceOf(DetachedObjectException.class, e.getCause());
    assertEquals(1, Database.count(emf));
  }

  @Test
  void removeOfMergedCopyDeletesDetachedEntity() {
    emf.runInTransaction(em -> em.remove(em.merge(brakes)));
    assertEquals(0, Database.count(emf));
  }

  @Test
  void removeOfTransientEntityIsIgnored() {
    ServiceOrder order = newOrder();
    emf.runInTransaction(em -> {
      em.remove(order);
      assertEquals(EntityState.TRANSIENT, stateOf(em, order));
    });
    assertTrue(sql.isEmpty());
  }

  @Test
  void persistOfDetachedEntityThrows() {
    EntityExistsException e = assertThrows(EntityExistsException.class,
        () -> emf.runInTransaction(em -> em.persist(brakes)));
    assertEquals("Detached entity passed to persist: com.howtodoinjava.hibernate.states.ServiceOrder",
        e.getMessage());
  }

  @Test
  void mergeOfRemovedEntityThrows() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> emf.runInTransaction(em -> {
          ServiceOrder order = em.find(ServiceOrder.class, brakes.getId());
          em.remove(order);
          em.merge(order);
        }));
    assertInstanceOf(ObjectDeletedException.class, e.getCause());
    assertEquals(1, Database.count(emf));
  }
}
