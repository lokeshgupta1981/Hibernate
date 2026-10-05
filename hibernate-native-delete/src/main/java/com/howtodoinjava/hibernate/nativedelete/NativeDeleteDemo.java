package com.howtodoinjava.hibernate.nativedelete;

import static com.howtodoinjava.hibernate.nativedelete.Database.TODAY;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/** Runs each native DELETE case and prints the SQL Hibernate sends. */
public class NativeDeleteDemo {

  public static void main(String[] args) {
    run("1. Native DELETE with a named parameter", false, emf -> {
      int deleted = emf.callInTransaction(em -> em
          .createNativeQuery("delete from coupon where expires_on < :today")
          .setParameter("today", TODAY)
          .executeUpdate());
      System.out.println("deleted = " + deleted + ", left = " + codes(emf));
    });

    run("2. @NamedNativeQuery Coupon.deleteExpired", false, emf -> {
      int deleted = emf.callInTransaction(em -> em
          .createNamedQuery("Coupon.deleteExpired")
          .setParameter("today", TODAY)
          .executeUpdate());
      System.out.println("deleted = " + deleted + ", left = " + codes(emf));
    });

    run("3. Positional parameter in Coupon.deleteByDiscount", false, emf -> {
      int deleted = emf.callInTransaction(em -> em
          .createNamedQuery("Coupon.deleteByDiscount")
          .setParameter(1, 50)
          .executeUpdate());
      System.out.println("deleted = " + deleted + ", left = " + codes(emf));
    });

    run("4. Foreign key violation: summer has redemptions", true, emf -> {
      try {
        emf.runInTransaction(em -> em
            .createNamedQuery("Coupon.deleteExpired")
            .setParameter("today", TODAY)
            .executeUpdate());
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
      System.out.println("left = " + codes(emf) + ", redemptions = " + Database.rows(emf, "redemption"));
    });

    run("5. Fix 1: delete the children first", true, emf -> {
      emf.runInTransaction(em -> {
        int children = em.createNativeQuery("""
                delete from redemption
                where coupon_id in (select id from coupon where expires_on < :today)""")
            .setParameter("today", TODAY)
            .executeUpdate();
        int parents = em.createNamedQuery("Coupon.deleteExpired")
            .setParameter("today", TODAY)
            .executeUpdate();
        System.out.println("redemptions deleted = " + children + ", coupons deleted = " + parents);
      });
      System.out.println("left = " + codes(emf) + ", redemptions = " + Database.rows(emf, "redemption"));
    });

    run("6. Fix 2: ON DELETE CASCADE on the foreign key", true, emf -> {
      Database.addOnDeleteCascade(emf);
      int deleted = emf.callInTransaction(em -> em
          .createNamedQuery("Coupon.deleteExpired")
          .setParameter("today", TODAY)
          .executeUpdate());
      System.out.println("deleted = " + deleted + ", left = " + codes(emf)
          + ", redemptions = " + Database.rows(emf, "redemption"));
    });

    run("7. em.remove() for comparison", true, emf -> {
      emf.runInTransaction(em -> em.remove(find(em, "summer")));
      System.out.println("left = " + codes(emf) + ", redemptions = " + Database.rows(emf, "redemption")
          + ", callbacks = " + Coupon.REMOVE_CALLBACKS);
    });

    run("8. Callbacks do not run for a native DELETE", false, emf -> {
      emf.runInTransaction(em -> {
        find(em, "summer");
        em.createNamedQuery("Coupon.deleteExpired").setParameter("today", TODAY).executeUpdate();
      });
      System.out.println("callbacks = " + Coupon.REMOVE_CALLBACKS);
    });

    run("9. Stale managed entity after the DELETE", false, emf -> {
      emf.runInTransaction(em -> {
        Coupon flash = find(em, "flash");
        em.createNamedQuery("Coupon.deleteExpired").setParameter("today", TODAY).executeUpdate();
        Coupon again = em.find(Coupon.class, flash.getId());
        System.out.println("find after delete = " + again + ", same object = " + (again == flash)
            + ", contains = " + em.contains(flash));
        try {
          em.refresh(flash);
        } catch (RuntimeException e) {
          System.out.println("refresh: " + e.getClass().getName() + ": " + e.getMessage());
        }
        em.clear();
        System.out.println("find after clear = " + em.find(Coupon.class, flash.getId()));
      });
    });

    run("10. Changing a stale entity after the DELETE", false, emf -> {
      try {
        emf.runInTransaction(em -> {
          Coupon flash = find(em, "flash");
          em.createNamedQuery("Coupon.deleteExpired").setParameter("today", TODAY).executeUpdate();
          flash.setExpiresOn(LocalDate.of(2026, 11, 30));
        });
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
    });

    run("11. Pending changes are flushed before the native DELETE", false, emf -> {
      int deleted = emf.callInTransaction(em -> {
        find(em, "flash").setExpiresOn(LocalDate.of(2026, 11, 30));
        return em.createNamedQuery("Coupon.deleteExpired").setParameter("today", TODAY).executeUpdate();
      });
      System.out.println("deleted = " + deleted + ", left = " + codes(emf));
    });

    run("12. JPQL bulk DELETE", true, emf -> {
      try {
        emf.runInTransaction(em -> em
            .createQuery("delete from Coupon c where c.expiresOn < :today")
            .setParameter("today", TODAY)
            .executeUpdate());
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
      System.out.println("left = " + codes(emf) + ", callbacks = " + Coupon.REMOVE_CALLBACKS);
    });

    run("13. @SoftDelete is bypassed by a native DELETE", false, emf -> {
      emf.runInTransaction(em -> {
        em.persist(new Campaign("Black Friday"));
        em.persist(new Campaign("Back to School"));
        em.persist(new Campaign("New Year"));
      });
      emf.runInTransaction(em -> em.remove(
          em.createQuery("from Campaign where name = 'Black Friday'", Campaign.class).getSingleResult()));
      emf.runInTransaction(em -> em
          .createQuery("delete from Campaign where name = 'Back to School'").executeUpdate());
      emf.runInTransaction(em -> em
          .createNativeQuery("delete from campaign where name = 'New Year'").executeUpdate());
      System.out.println("campaign rows = " + Database.rows(emf, "campaign")
          + ", visible campaigns = " + Database.count(emf, "Campaign"));
    });

    run("14. executeUpdate() without a transaction", false, emf -> {
      try (EntityManager em = emf.createEntityManager()) {
        em.createNamedQuery("Coupon.deleteExpired").setParameter("today", TODAY).executeUpdate();
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
    });
  }

  static Coupon find(EntityManager em, String code) {
    return em.createQuery("from Coupon where code = :code", Coupon.class)
        .setParameter("code", code)
        .getSingleResult();
  }

  static List<String> codes(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> em
        .createNativeQuery("select code from coupon order by id", String.class)
        .getResultList());
  }

  private static void run(String title, boolean summerRedeemed, Consumer<EntityManagerFactory> body) {
    Coupon.REMOVE_CALLBACKS.clear();
    try (EntityManagerFactory emf = Database.create(true)) {
      Database.seed(emf, summerRedeemed);
      System.out.println();
      System.out.println("=== " + title + " ===");
      body.accept(emf);
    }
  }
}
