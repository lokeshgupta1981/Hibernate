package com.howtodoinjava.hibernate.delete;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.function.Consumer;

public class DeleteEntitiesDemo {

  public static void main(String[] args) {

    scenario("1. find() then remove()", emf -> {
      Long alexId = Database.subscriberId(emf, "Alex");
      emf.runInTransaction(em -> {
        Subscriber alex = em.find(Subscriber.class, alexId);
        em.remove(alex);
        System.out.println("contains(alex) = " + em.contains(alex));
        System.out.println("find again = " + em.find(Subscriber.class, alexId));
        System.out.println("-- commit --");
      });
    });

    scenario("2. getReference() then remove()", emf -> {
      Long alexId = Database.subscriberId(emf, "Alex");
      Database.resetStatistics(emf);
      emf.runInTransaction(em -> em.remove(em.getReference(Subscriber.class, alexId)));
      System.out.println("statements = " + Database.statements(emf));
    });

    scenario("3. remove() on a detached entity", emf -> {
      Long alexId = Database.subscriberId(emf, "Alex");
      Subscriber detached = emf.callInTransaction(em -> em.find(Subscriber.class, alexId));
      try {
        emf.runInTransaction(em -> em.remove(detached));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
      System.out.println("-- fix: remove the managed copy --");
      emf.runInTransaction(em -> em.remove(em.merge(detached)));
    });

    scenario("4. Bulk delete with JPQL", emf -> {
      emf.runInTransaction(em -> {
        int deleted = em.createQuery("delete from Subscriber s where s.confirmed = false")
            .executeUpdate();
        System.out.println("deleted = " + deleted);
      });
    });

    scenario("5. Bulk delete leaves loaded entities in the persistence context", emf -> {
      Long alexId = Database.subscriberId(emf, "Alex");
      emf.runInTransaction(em -> {
        Subscriber alex = em.find(Subscriber.class, alexId);
        em.createQuery("delete from Subscriber s where s.confirmed = false").executeUpdate();
        System.out.println("contains(alex) = " + em.contains(alex));
        System.out.println("find again = " + em.find(Subscriber.class, alexId));
        em.clear();
        System.out.println("after clear(), find = " + em.find(Subscriber.class, alexId));
      });
    });

    scenario("6. Removing the stale entity after a bulk delete", emf -> {
      Long alexId = Database.subscriberId(emf, "Alex");
      try {
        emf.runInTransaction(em -> {
          Subscriber alex = em.find(Subscriber.class, alexId);
          em.createQuery("delete from Subscriber s where s.confirmed = false").executeUpdate();
          em.remove(alex);
        });
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
    });

    scenario("7. remove() on a newsletter that still has subscribers", emf -> {
      Long newsletterId = emf.callInTransaction(em ->
          em.createQuery("select n.id from Newsletter n", Long.class).getSingleResult());
      try {
        emf.runInTransaction(em -> em.remove(em.find(Newsletter.class, newsletterId)));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
        System.out.println("cause: " + e.getCause().getClass().getName());
      }
    });

    scenario("8. Bulk delete of a newsletter that still has subscribers", emf -> {
      try {
        emf.runInTransaction(em -> em.createQuery("delete from Newsletter").executeUpdate());
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
    });

    scenario("9. Delete the subscribers first, then the newsletter", emf -> {
      Long newsletterId = emf.callInTransaction(em ->
          em.createQuery("select n.id from Newsletter n", Long.class).getSingleResult());
      emf.runInTransaction(em -> {
        Newsletter javaWeekly = em.find(Newsletter.class, newsletterId);
        em.createQuery("delete from Subscriber s where s.newsletter = :newsletter")
            .setParameter("newsletter", javaWeekly)
            .executeUpdate();
        em.remove(javaWeekly);
      });
    });

    scenario("10. Statement count: remove() in a loop vs bulk delete", emf -> {
      Database.resetStatistics(emf);
      emf.runInTransaction(em -> {
        List<Subscriber> unconfirmed = em
            .createQuery("from Subscriber s where s.confirmed = false", Subscriber.class)
            .getResultList();
        unconfirmed.forEach(em::remove);
      });
      System.out.println("remove() loop statements = " + Database.statements(emf));
    });
  }

  private static void scenario(String title, Consumer<EntityManagerFactory> body) {
    System.out.println();
    System.out.println("== " + title + " ==");
    try (EntityManagerFactory emf = Database.create(true)) {
      Database.seed(emf);
      System.out.println("-- start --");
      body.accept(emf);
      System.out.println("subscribers=" + Database.count(emf, "Subscriber")
          + " newsletters=" + Database.count(emf, "Newsletter"));
    }
  }
}
