package com.howtodoinjava.hibernate.mergerefresh;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;

public class MergeRefreshDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Persist a shipment; it is detached after the transaction");
      Shipment pune = new Shipment("Pune", "packed");
      emf.runInTransaction(em -> em.persist(pune));
      print("pune", pune);

      step("2. Change the detached object: nothing is saved");
      pune.setStatus("shipped");
      print("row", Database.load(emf, pune.getId()));

      step("3. merge() the detached object");
      Shipment merged = emf.callInTransaction(em -> {
        Shipment copy = em.merge(pune);
        System.out.println("copy == pune      : " + (copy == pune));
        System.out.println("em.contains(copy) : " + em.contains(copy));
        System.out.println("em.contains(pune) : " + em.contains(pune));
        System.out.println("-- commit");
        return copy;
      });
      print("copy", merged);
      print("pune", pune);

      step("4. Change the argument after merge(): ignored");
      emf.runInTransaction(em -> {
        Shipment copy = em.merge(merged);
        merged.setDestination("Mumbai");
        System.out.println("copy == merged    : " + (copy == merged));
      });
      print("row", Database.load(emf, pune.getId()));
      merged.setDestination("Pune");

      step("5. merge() when the entity is already loaded");
      Shipment detached = Database.load(emf, pune.getId());
      detached.setStatus("in transit");
      emf.runInTransaction(em -> {
        Shipment loaded = em.find(Shipment.class, pune.getId());
        System.out.println("-- merge");
        Shipment copy = em.merge(detached);
        System.out.println("copy == loaded    : " + (copy == loaded));
        System.out.println("loaded.status     : " + loaded.getStatus());
      });

      step("6. merge() a new object");
      Shipment delhi = new Shipment("Delhi", "packed");
      Shipment saved = emf.callInTransaction(em -> em.merge(delhi));
      print("delhi", delhi);
      print("saved", saved);

      step("7. merge() an outdated copy: OptimisticLockException");
      try {
        emf.runInTransaction(em -> em.merge(pune));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("7b. persist() a detached object");
      try {
        emf.runInTransaction(em -> em.persist(pune));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("8. refresh() discards unsaved changes");
      emf.runInTransaction(em -> {
        Shipment shipment = em.find(Shipment.class, pune.getId());
        shipment.setStatus("lost");
        em.refresh(shipment);
        System.out.println("status            : " + shipment.getStatus());
        System.out.println("-- commit");
      });

      step("8b. refresh() right after merge(): the merged change is lost");
      Shipment late = Database.load(emf, pune.getId());
      late.setStatus("on hold");
      emf.runInTransaction(em -> {
        Shipment copy = em.merge(late);
        em.refresh(copy);
        System.out.println("status            : " + copy.getStatus());
      });

      step("8c. flush() before refresh() keeps it");
      emf.runInTransaction(em -> {
        Shipment copy = em.merge(late);
        em.flush();
        em.refresh(copy);
        System.out.println("status            : " + copy.getStatus());
      });

      step("9. refresh() after a bulk update");
      emf.runInTransaction(em -> {
        Shipment shipment = em.find(Shipment.class, pune.getId());
        int rows = em.createQuery("update Shipment set status = 'delivered' where destination = 'Pune'")
            .executeUpdate();
        System.out.println("rows              : " + rows);
        System.out.println("find().status     : " + em.find(Shipment.class, pune.getId()).getStatus());
        em.refresh(shipment);
        System.out.println("status            : " + shipment.getStatus());
      });

      step("10. refresh() after a native SQL update");
      emf.runInTransaction(em -> {
        Shipment shipment = em.find(Shipment.class, saved.getId());
        em.createNativeQuery("update Shipment set status = 'shipped' where id = ?")
            .setParameter(1, saved.getId())
            .executeUpdate();
        em.refresh(shipment);
        System.out.println("status            : " + shipment.getStatus());
      });

      step("11. refresh() a detached object");
      try {
        emf.runInTransaction(em -> em.refresh(pune));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("12. refresh() to read a value set by a trigger");
      emf.runInTransaction(em -> {
        Shipment chennai = new Shipment("Chennai", null);
        em.persist(chennai);
        em.flush();
        System.out.println("status            : " + chennai.getStatus());
        em.refresh(chennai);
        System.out.println("status            : " + chennai.getStatus());
      });

      step("13. Session.merge() replaces Session.update()");
      Shipment outdated = Database.load(emf, saved.getId());
      outdated.setStatus("delivered");
      emf.runInTransaction(em -> em.unwrap(Session.class).merge(outdated));
      print("row", Database.load(emf, saved.getId()));

      step("14. merge() a half-filled object");
      Shipment goa = new Shipment("Goa", "packed");
      emf.runInTransaction(em -> em.persist(goa));
      Shipment form = new Shipment(null, "returned");
      form.setId(goa.getId());
      emf.runInTransaction(em -> em.merge(form));
      print("row", Database.load(emf, goa.getId()));

      step("15. refresh() with a lock");
      emf.runInTransaction(em -> {
        Shipment shipment = em.find(Shipment.class, saved.getId());
        em.refresh(shipment, LockModeType.PESSIMISTIC_WRITE);
      });

      step("16. merge() a detached object whose row was deleted");
      Shipment gone = Database.load(emf, saved.getId());
      emf.runInTransaction(em -> em.remove(em.find(Shipment.class, saved.getId())));
      try {
        emf.runInTransaction(em -> em.merge(gone));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title);
  }

  private static void print(String label, Shipment shipment) {
    System.out.println(label + " = " + shipment);
  }
}
