package com.howtodoinjava.hibernate.states;

import static com.howtodoinjava.hibernate.states.Database.stateOf;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.Session;

public class EntityStatesDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Transient: new object, then persist() and commit");
      ServiceOrder order = new ServiceOrder("1234", "Brake noise", "open", new BigDecimal("40.00"));
      EntityManager em = emf.createEntityManager();
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      print("state before persist", stateOf(em, order));
      print("id before persist", emf.getPersistenceUnitUtil().getIdentifier(order));
      em.persist(order);
      print("state after persist", stateOf(em, order));
      print("id after persist", order.getId());
      System.out.println("-- commit");
      tx.commit();

      step("2. Managed: change a field, no method call");
      tx.begin();
      order.setStatus("in progress");
      print("session.isDirty()", em.unwrap(Session.class).isDirty());
      System.out.println("-- commit");
      tx.commit();
      print("isDirty after commit", em.unwrap(Session.class).isDirty());

      step("3. Detached: em.close()");
      em.close();
      order.setCost(new BigDecimal("55.00"));
      print("cost in database", Database.load(emf, order.getId()).getCost());

      step("4. merge() the detached object");
      Long id = order.getId();
      ServiceOrder managedCopy = emf.callInTransaction(em2 -> {
        ServiceOrder copy = em2.merge(order);
        print("copy == order", copy == order);
        print("state of order", stateOf(em2, order));
        print("state of copy", stateOf(em2, copy));
        System.out.println("-- commit");
        return copy;
      });
      print("cost in database", Database.load(emf, id).getCost());

      step("5. find() and queries return managed entities");
      emf.runInTransaction(em2 -> {
        ServiceOrder found = em2.find(ServiceOrder.class, id);
        print("state after find", stateOf(em2, found));
        List<ServiceOrder> open = em2.createQuery(
            "from ServiceOrder where status = :status", ServiceOrder.class)
            .setParameter("status", "in progress").getResultList();
        print("same object from query", open.get(0) == found);
        em2.detach(found);
        print("state after detach", stateOf(em2, found));
        found.setStatus("lost");
        ServiceOrder again = em2.find(ServiceOrder.class, id);
        print("again == found", again == found);
        em2.clear();
        print("state after clear", stateOf(em2, again));
        System.out.println("-- commit");
      });

      step("6. remove() a managed entity, then persist() it again");
      emf.runInTransaction(em2 -> {
        ServiceOrder found = em2.find(ServiceOrder.class, id);
        em2.remove(found);
        print("state after remove", stateOf(em2, found));
        print("contains after remove", em2.contains(found));
        em2.persist(found);
        print("state after persist again", stateOf(em2, found));
        System.out.println("-- commit");
      });
      print("rows", Database.count(emf));

      step("7. remove() and commit");
      emf.runInTransaction(em2 -> {
        ServiceOrder found = em2.find(ServiceOrder.class, id);
        em2.remove(found);
        print("found.getId()", found.getId());
        System.out.println("-- commit");
      });
      print("rows", Database.count(emf));

      step("8. remove() a detached entity");
      ServiceOrder second = new ServiceOrder("5678", "Oil change", "open", new BigDecimal("25.00"));
      emf.runInTransaction(em2 -> em2.persist(second));
      try {
        emf.runInTransaction(em2 -> em2.remove(second));
      } catch (RuntimeException e) {
        print("exception", e.getClass().getName() + ": " + e.getMessage());
      }
      emf.runInTransaction(em2 -> em2.remove(em2.merge(second)));
      print("rows", Database.count(emf));

      step("9. persist() a detached entity");
      ServiceOrder third = new ServiceOrder("9012", "Flat tyre", "open", new BigDecimal("15.00"));
      emf.runInTransaction(em2 -> em2.persist(third));
      try {
        emf.runInTransaction(em2 -> em2.persist(third));
      } catch (RuntimeException e) {
        print("exception", e.getClass().getName() + ": " + e.getMessage());
      }

      step("10. remove() a transient entity");
      emf.runInTransaction(em2 -> {
        ServiceOrder fresh = new ServiceOrder("3456", "Wipers", "open", new BigDecimal("10.00"));
        em2.remove(fresh);
        print("state", stateOf(em2, fresh));
      });

      step("11. merge() a removed entity");
      try {
        emf.runInTransaction(em2 -> {
          ServiceOrder found = em2.find(ServiceOrder.class, third.getId());
          em2.remove(found);
          em2.merge(found);
        });
      } catch (RuntimeException e) {
        print("exception", e.getClass().getName() + ": " + e.getMessage());
      }

      step("12. merge() a transient entity");
      emf.runInTransaction(em2 -> {
        ServiceOrder fresh = new ServiceOrder("7890", "Battery", "open", new BigDecimal("90.00"));
        ServiceOrder copy = em2.merge(fresh);
        print("fresh state", stateOf(em2, fresh));
        print("copy state", stateOf(em2, copy));
        System.out.println("-- commit");
      });

      step("13. flush() sends the SQL before commit");
      emf.runInTransaction(em2 -> {
        ServiceOrder found = em2.find(ServiceOrder.class, third.getId());
        print("isDirty before change", em2.unwrap(Session.class).isDirty());
        found.setStatus("done");
        System.out.println("-- flush");
        em2.flush();
        print("isDirty after flush", em2.unwrap(Session.class).isDirty());
        System.out.println("-- commit");
      });
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }

  private static void print(String label, Object value) {
    System.out.println(label + ": " + value);
  }
}
