package com.howtodoinjava.hibernate.reference;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Timeout;
import org.hibernate.Hibernate;

public class FindVsGetReferenceDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("0. Save an agent and a ticket");
      Long agentId = emf.callInTransaction(em -> {
        Agent lokesh = new Agent("Lokesh");
        em.persist(lokesh);
        em.persist(new Ticket("Printer is offline"));
        return lokesh.getId();
      });
      Long ticketId = emf.callInTransaction(em ->
          em.createQuery("select id from Ticket", Long.class).getSingleResult());
      System.out.println("agentId=" + agentId + " ticketId=" + ticketId);

      step("1. find() an existing agent");
      emf.runInTransaction(em -> {
        Agent agent = em.find(Agent.class, agentId);
        System.out.println("class=" + agent.getClass().getSimpleName()
            + " name=" + agent.getName());
        Agent again = em.find(Agent.class, agentId);
        System.out.println("second find, same object: " + (agent == again));
      });

      step("2. find() a missing agent");
      emf.runInTransaction(em -> {
        Agent agent = em.find(Agent.class, 99L);
        System.out.println("agent=" + agent);
      });

      step("3. getReference() an existing agent");
      emf.runInTransaction(em -> {
        Agent agent = em.getReference(Agent.class, agentId);
        System.out.println("after getReference");
        System.out.println("class=" + agent.getClass().getName());
        System.out.println("id=" + agent.getId()
            + " initialized=" + Hibernate.isInitialized(agent));
        System.out.println("calling getName()");
        System.out.println("name=" + agent.getName()
            + " initialized=" + Hibernate.isInitialized(agent));
        System.out.println("instanceof Agent: " + (agent instanceof Agent)
            + " Hibernate.getClass=" + Hibernate.getClass(agent).getSimpleName());
      });

      step("4. Assign a new ticket with getReference()");
      emf.runInTransaction(em -> {
        Ticket ticket = new Ticket("VPN keeps disconnecting");
        ticket.setAssignee(em.getReference(Agent.class, agentId));
        em.persist(ticket);
      });

      step("5. Assign a new ticket with find()");
      emf.runInTransaction(em -> {
        Ticket ticket = new Ticket("Laptop will not start");
        ticket.setAssignee(em.find(Agent.class, agentId));
        em.persist(ticket);
      });

      step("6. Assign an existing ticket with getReference()");
      emf.runInTransaction(em -> {
        Ticket ticket = em.find(Ticket.class, ticketId);
        ticket.setAssignee(em.getReference(Agent.class, agentId));
      });

      step("7. getReference() a missing agent, then read its name");
      try {
        emf.runInTransaction(em -> {
          Agent agent = em.getReference(Agent.class, 99L);
          System.out.println("got a reference, id=" + agent.getId());
          agent.getName();
        });
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("8. Assign a ticket to a missing agent with getReference()");
      try {
        emf.runInTransaction(em -> {
          Ticket ticket = new Ticket("Mouse is not working");
          ticket.setAssignee(em.getReference(Agent.class, 99L));
          em.persist(ticket);
        });
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("9. Read a reference after the EntityManager is closed");
      Agent detached = emf.callInTransaction(em -> em.getReference(Agent.class, agentId));
      try {
        detached.getName();
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("10. getReference() after find(), and find() after getReference()");
      emf.runInTransaction(em -> {
        Agent found = em.find(Agent.class, agentId);
        Agent ref = em.getReference(Agent.class, agentId);
        System.out.println("getReference returns the loaded entity: " + (found == ref));
      });
      emf.runInTransaction(em -> {
        Agent ref = em.getReference(Agent.class, agentId);
        Agent found = em.find(Agent.class, agentId);
        System.out.println("find returns the proxy: " + (found == ref)
            + " initialized=" + Hibernate.isInitialized(found));
      });

      step("11. Change and remove through a reference");
      Long alexId = emf.callInTransaction(em -> {
        Agent alex = new Agent("Alex");
        em.persist(alex);
        return alex.getId();
      });
      emf.runInTransaction(em -> em.getReference(Agent.class, agentId).setName("Lokesh Gupta"));
      emf.runInTransaction(em -> em.remove(em.getReference(Agent.class, alexId)));

      step("12. JPA 3.2: find() with options");
      emf.runInTransaction(em -> {
        Agent agent = em.find(Agent.class, agentId,
            LockModeType.PESSIMISTIC_WRITE, Timeout.seconds(2));
        System.out.println("locked " + agent.getName());
      });

      step("13. JPA 3.2: find() with an EntityGraph");
      emf.runInTransaction(em -> {
        EntityGraph<Ticket> graph = em.createEntityGraph(Ticket.class);
        graph.addAttributeNode("assignee");
        Ticket ticket = em.find(graph, ticketId);
        System.out.println("assignee initialized="
            + Hibernate.isInitialized(ticket.getAssignee())
            + " name=" + ticket.getAssignee().getName());
      });

      step("14. JPA 3.2: getReference(entity) for a detached agent");
      Agent loaded = emf.callInTransaction(em -> em.find(Agent.class, agentId));
      emf.runInTransaction(em -> {
        Ticket ticket = new Ticket("Monitor is flickering");
        ticket.setAssignee(em.getReference(loaded));
        em.persist(ticket);
      });

      step("15. Fix the detached reference: initialize it before the EntityManager closes");
      Agent ready = emf.callInTransaction(em -> {
        Agent agent = em.getReference(Agent.class, agentId);
        Hibernate.initialize(agent);
        return agent;
      });
      System.out.println("name=" + ready.getName());
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
