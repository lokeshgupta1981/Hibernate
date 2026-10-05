package com.howtodoinjava.hibernate.callbacks;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;

public class CallbacksDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Persist an expense claim (IDENTITY id)");
      Long id = emf.callInTransaction(em -> {
        ExpenseClaim lunch = new ExpenseClaim("Team lunch", new BigDecimal("40.00"));
        mark("em.persist(lunch)");
        em.persist(lunch);
        mark("persist() returned, id = " + lunch.getId() + ", status = " + lunch.getStatus());
        mark("commit");
        return lunch.getId();
      });
      mark("committed");

      step("2. Persist a travel claim (SEQUENCE id, @CreationTimestamp)");
      emf.runInTransaction(em -> {
        TravelClaim taxi = new TravelClaim("Taxi", new BigDecimal("25.00"));
        mark("em.persist(taxi)");
        em.persist(taxi);
        mark("persist() returned, id = " + taxi.getId() + ", submittedAt = " + taxi.getSubmittedAt());
        mark("em.flush()");
        em.flush();
        mark("flush() returned, submittedAt set = " + (taxi.getSubmittedAt() != null));
        mark("commit");
      });
      mark("committed");

      step("3. Load and approve the expense claim");
      emf.runInTransaction(em -> {
        mark("em.find()");
        ExpenseClaim lunch = em.find(ExpenseClaim.class, id);
        mark("lunch.setStatus(APPROVED)");
        lunch.setStatus(ClaimStatus.APPROVED);
        mark("commit");
      });
      mark("committed");

      step("4. Refresh and query");
      emf.runInTransaction(em -> {
        ExpenseClaim lunch = em.getReference(ExpenseClaim.class, id);
        mark("em.refresh(lunch)");
        em.refresh(lunch);
        mark("select c from ExpenseClaim c");
        em.createQuery("select c from ExpenseClaim c", ExpenseClaim.class).getResultList();
      });

      step("5. Remove the expense claim");
      emf.runInTransaction(em -> {
        ExpenseClaim lunch = em.find(ExpenseClaim.class, id);
        mark("em.remove(lunch)");
        em.remove(lunch);
        mark("commit");
      });
      mark("committed");

      step("6. A listener rejects a negative amount");
      try {
        emf.runInTransaction(em -> em.persist(new ExpenseClaim("Coffee", new BigDecimal("-5.00"))));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("7. Bulk JPQL update skips @PreUpdate");
      Long hotelId = save(emf, "Hotel", "90.00");
      emf.runInTransaction(em -> {
        int rows = em.createQuery("update ExpenseClaim c set c.status = :s where c.id = :id")
            .setParameter("s", ClaimStatus.APPROVED).setParameter("id", hotelId).executeUpdate();
        mark("rows updated = " + rows);
      });

    }

    try (EntityManagerFactory emf = Database.createRisky(true)) {
      Long hotelId = save(emf, "Hotel", "90.00");

      step("8. Running a query in @PreUpdate");
      CallbackLog.print(false);
      runRisky(emf, RiskyListener.Mode.QUERY_IN_PRE_UPDATE, hotelId);
      CallbackLog.print(true);
      System.out.println("description in db = " + description(emf, hotelId));

      step("9. Calling em.persist() in @PostPersist");
      Long trainId = emf.callInTransaction(em -> {
        RiskyListener.em = em;
        RiskyListener.mode = RiskyListener.Mode.PERSIST_IN_POST_PERSIST;
        ExpenseClaim train = new ExpenseClaim("Train ticket", new BigDecimal("30.00"));
        em.persist(train);
        mark("commit");
        return train.getId();
      });
      RiskyListener.mode = RiskyListener.Mode.OFF;
      System.out.println("history rows = " + emf.callInTransaction(em ->
          em.createQuery("select count(h) from ClaimHistory h", Long.class).getSingleResult()));

      step("10. Changing the entity in @PostPersist (IDENTITY)");
      Long parkingId = emf.callInTransaction(em -> {
        RiskyListener.mode = RiskyListener.Mode.CHANGE_IN_POST_PERSIST;
        ExpenseClaim parking = new ExpenseClaim("Parking", new BigDecimal("8.00"));
        em.persist(parking);
        mark("commit");
        return parking.getId();
      });
      RiskyListener.mode = RiskyListener.Mode.OFF;
      System.out.println("description in db = " + description(emf, parkingId));

      step("11. Changing the entity in @PostUpdate");
      runRisky(emf, RiskyListener.Mode.CHANGE_IN_POST_UPDATE, trainId);
      System.out.println("description in db = " + description(emf, trainId));
    }
  }

  static Long save(EntityManagerFactory emf, String description, String amount) {
    return emf.callInTransaction(em -> {
      ExpenseClaim claim = new ExpenseClaim(description, new BigDecimal(amount));
      em.persist(claim);
      return claim.getId();
    });
  }

  static void runRisky(EntityManagerFactory emf, RiskyListener.Mode mode, Long id) {
    try {
      emf.runInTransaction(em -> {
        RiskyListener.em = em;
        RiskyListener.mode = mode;
        em.find(ExpenseClaim.class, id).setAmount(new BigDecimal("95.00"));
        mark("commit");
      });
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
    } finally {
      RiskyListener.mode = RiskyListener.Mode.OFF;
    }
  }

  static String description(EntityManagerFactory emf, Long id) {
    return emf.callInTransaction(em -> em.find(ExpenseClaim.class, id).getDescription());
  }

  static void mark(String text) {
    CallbackLog.add("-- " + text);
  }

  static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }
}
