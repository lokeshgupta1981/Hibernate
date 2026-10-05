package com.howtodoinjava.hibernate.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import jakarta.persistence.RollbackException;
import jakarta.persistence.Timeout;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FindVsGetReferenceTest {

  private EntityManagerFactory emf;
  private Long agentId;
  private Long ticketId;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    Long[] ids = emf.callInTransaction(em -> {
      Agent lokesh = new Agent("Lokesh");
      em.persist(lokesh);
      Ticket ticket = new Ticket("Printer is offline");
      em.persist(ticket);
      return new Long[] {lokesh.getId(), ticket.getId()};
    });
    agentId = ids[0];
    ticketId = ids[1];
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private long assigneeCount() {
    return emf.callInTransaction(em -> em
        .createQuery("select count(*) from Ticket where assignee.id = :id", Long.class)
        .setParameter("id", agentId)
        .getSingleResult());
  }

  @Test
  void findRunsSelectAndReturnsRealEntity() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Agent agent = em.find(Agent.class, agentId);
      assertEquals(1, stats.getPrepareStatementCount());
      assertEquals(Agent.class, agent.getClass());
      assertEquals("Lokesh", agent.getName());
    });
  }

  @Test
  void secondFindComesFromPersistenceContext() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Agent first = em.find(Agent.class, agentId);
      Agent second = em.find(Agent.class, agentId);
      assertSame(first, second);
    });
    assertEquals(1, stats.getPrepareStatementCount());
  }

  @Test
  void findReturnsNullForMissingRow() {
    emf.runInTransaction(em -> assertNull(em.find(Agent.class, 99L)));
  }

  @Test
  void getReferenceReturnsProxyWithoutSql() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Agent agent = em.getReference(Agent.class, agentId);
      assertEquals(0, stats.getPrepareStatementCount());
      assertNotEquals(Agent.class, agent.getClass());
      assertTrue(agent.getClass().getName().endsWith("Agent$HibernateProxy"));
      assertTrue(agent instanceof Agent);
      assertFalse(Hibernate.isInitialized(agent));
      assertEquals(0, stats.getPrepareStatementCount());
    });
  }

  @Test
  void hibernateGetClassInitializesTheProxy() {
    emf.runInTransaction(em -> {
      Agent agent = em.getReference(Agent.class, agentId);
      assertEquals(Agent.class, Hibernate.getClass(agent));
      assertTrue(Hibernate.isInitialized(agent));
    });
  }

  @Test
  void getIdDoesNotLoadTheProxy() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Agent agent = em.getReference(Agent.class, agentId);
      assertEquals(agentId, agent.getId());
      assertFalse(Hibernate.isInitialized(agent));
      assertEquals(0, stats.getPrepareStatementCount());
    });
  }

  @Test
  void firstGetterCallLoadsTheProxy() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Agent agent = em.getReference(Agent.class, agentId);
      assertEquals("Lokesh", agent.getName());
      assertTrue(Hibernate.isInitialized(agent));
      assertEquals(1, stats.getPrepareStatementCount());
    });
  }

  @Test
  void assigningWithGetReferenceRunsOnlyInsert() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Ticket ticket = new Ticket("VPN keeps disconnecting");
      ticket.setAssignee(em.getReference(Agent.class, agentId));
      em.persist(ticket);
    });
    assertEquals(1, stats.getPrepareStatementCount());
    assertEquals(1, stats.getEntityInsertCount());
    assertEquals(0, stats.getEntityLoadCount());
    assertEquals(1, assigneeCount());
  }

  @Test
  void assigningWithFindRunsSelectAndInsert() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Ticket ticket = new Ticket("Laptop will not start");
      ticket.setAssignee(em.find(Agent.class, agentId));
      em.persist(ticket);
    });
    assertEquals(2, stats.getPrepareStatementCount());
    assertEquals(1, stats.getEntityLoadCount());
    assertEquals(1, assigneeCount());
  }

  @Test
  void assigningExistingTicketWithGetReferenceLoadsOnlyTicket() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Ticket ticket = em.find(Ticket.class, ticketId);
      ticket.setAssignee(em.getReference(Agent.class, agentId));
    });
    assertEquals(2, stats.getPrepareStatementCount()); // select ticket + update ticket
    assertEquals(1, stats.getEntityLoadCount());
    assertEquals(1, stats.getEntityUpdateCount());
    assertEquals(1, assigneeCount());
  }

  @Test
  void missingRowFailsOnFirstAccessNotOnGetReference() {
    emf.runInTransaction(em -> {
      Agent agent = em.getReference(Agent.class, 99L);
      assertEquals(99L, agent.getId());
      EntityNotFoundException e = assertThrows(EntityNotFoundException.class, agent::getName);
      assertEquals("No row with the given identifier exists for entity "
          + "[com.howtodoinjava.hibernate.reference.Agent with id '99']", e.getMessage());
    });
  }

  @Test
  void missingRowAsForeignKeyFailsOnInsert() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Ticket ticket = new Ticket("Mouse is not working");
      ticket.setAssignee(em.getReference(Agent.class, 99L));
      ConstraintViolationException e =
          assertThrows(ConstraintViolationException.class, () -> em.persist(ticket));
      assertTrue(e.getMessage().contains("Referential integrity constraint violation"));
      em.getTransaction().setRollbackOnly();
    });
    assertEquals(0, stats.getEntityLoadCount());
  }

  @Test
  void proxyCannotLoadAfterClose() {
    Agent detached = emf.callInTransaction(em -> em.getReference(Agent.class, agentId));
    LazyInitializationException e =
        assertThrows(LazyInitializationException.class, detached::getName);
    assertEquals("Could not initialize proxy [com.howtodoinjava.hibernate.reference.Agent#1]"
        + " - no session", e.getMessage());
    assertEquals(agentId, detached.getId());
  }

  @Test
  void initializedProxyWorksAfterClose() {
    Agent ready = emf.callInTransaction(em -> {
      Agent agent = em.getReference(Agent.class, agentId);
      Hibernate.initialize(agent);
      return agent;
    });
    assertEquals("Lokesh", ready.getName());
  }

  @Test
  void getReferenceAfterFindReturnsTheLoadedEntity() {
    emf.runInTransaction(em -> {
      Agent found = em.find(Agent.class, agentId);
      Agent ref = em.getReference(Agent.class, agentId);
      assertSame(found, ref);
      assertEquals(Agent.class, ref.getClass());
    });
  }

  @Test
  void findAfterGetReferenceReturnsTheInitializedProxy() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Agent ref = em.getReference(Agent.class, agentId);
      Agent found = em.find(Agent.class, agentId);
      assertSame(ref, found);
      assertTrue(Hibernate.isInitialized(found));
      assertEquals(1, stats.getPrepareStatementCount());
    });
  }

  @Test
  void changingProxyStateLoadsAndUpdates() {
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> em.getReference(Agent.class, agentId).setName("Lokesh Gupta"));
    assertEquals(2, stats.getPrepareStatementCount()); // select + update
    assertEquals(1, stats.getEntityUpdateCount());
  }

  @Test
  void removingProxyRunsOnlyDelete() {
    Long otherId = emf.callInTransaction(em -> {
      Agent alex = new Agent("Alex");
      em.persist(alex);
      return alex.getId();
    });
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> em.remove(em.getReference(Agent.class, otherId)));
    assertEquals(0, stats.getEntityLoadCount());
    assertEquals(1, stats.getEntityDeleteCount());
    assertEquals(1, stats.getPrepareStatementCount()); // delete only
  }

  @Test
  void findWithLockAndTimeoutOptions() {
    emf.runInTransaction(em -> {
      Agent agent = em.find(Agent.class, agentId, LockModeType.PESSIMISTIC_WRITE,
          Timeout.seconds(2));
      assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(agent));
    });
  }

  @Test
  void findWithEntityGraphLoadsAssigneeInOneSelect() {
    emf.runInTransaction(em -> em.find(Ticket.class, ticketId)
        .setAssignee(em.getReference(Agent.class, agentId)));
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      EntityGraph<Ticket> graph = em.createEntityGraph(Ticket.class);
      graph.addAttributeNode("assignee");
      Ticket ticket = em.find(graph, ticketId);
      assertTrue(Hibernate.isInitialized(ticket.getAssignee()));
      assertEquals("Lokesh", ticket.getAssignee().getName());
    });
    assertEquals(1, stats.getPrepareStatementCount());
  }

  @Test
  void getReferenceFromDetachedEntityRunsNoSelect() {
    Agent loaded = emf.callInTransaction(em -> em.find(Agent.class, agentId));
    Statistics stats = Database.statistics(emf);
    emf.runInTransaction(em -> {
      Ticket ticket = new Ticket("Monitor is flickering");
      ticket.setAssignee(em.getReference(loaded));
      em.persist(ticket);
    });
    assertEquals(0, stats.getEntityLoadCount());
    assertEquals(1, stats.getEntityInsertCount());
    assertEquals(1, assigneeCount());
  }

  @Test
  void removingMissingRowThroughProxy() {
    Statistics stats = Database.statistics(emf);
    RollbackException e = assertThrows(RollbackException.class, () ->
        emf.runInTransaction(em -> em.remove(em.getReference(Agent.class, 99L))));
    assertTrue(e.getMessage().contains("Unexpected row count (expected row count 1 but was 0)"));
    assertEquals(1, stats.getPrepareStatementCount()); // the delete only
  }

  @Test
  void missingRowWithFindAssignsNull() {
    Long id = emf.callInTransaction(em -> {
      Ticket ticket = new Ticket("Mouse is not working");
      ticket.setAssignee(em.find(Agent.class, 99L));
      em.persist(ticket);
      return ticket.getId();
    });
    emf.runInTransaction(em -> assertNull(em.find(Ticket.class, id).getAssignee()));
  }
}
