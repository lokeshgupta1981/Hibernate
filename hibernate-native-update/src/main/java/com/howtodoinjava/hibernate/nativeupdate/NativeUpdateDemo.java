package com.howtodoinjava.hibernate.nativeupdate;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.query.NativeQuery;
import org.hibernate.stat.Statistics;

public class NativeUpdateDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Save four plans and one add-on");
      Map<String, Long> ids = new LinkedHashMap<>(Database.seed(emf));
      plans(emf);

      step("2. createNativeQuery(): raise the price of every active plan by 1.00");
      int raised = emf.callInTransaction(em -> em
          .createNativeQuery("update plan set monthly_price = monthly_price + :amount where active = true")
          .setParameter("amount", new BigDecimal("1.00"))
          .executeUpdate());
      System.out.println("rows updated = " + raised);
      plans(emf);

      step("3. Positional parameters: rename 'Legacy HD' to 'Legacy'");
      int renamed = emf.callInTransaction(em -> em
          .createNativeQuery("update plan set name = ?1 where name = ?2")
          .setParameter(1, "Legacy")
          .setParameter(2, "Legacy HD")
          .executeUpdate());
      System.out.println("rows updated = " + renamed);

      step("4. @NamedNativeQuery Plan.deactivateCheaperThan(10.00)");
      int deactivated = emf.callInTransaction(em -> em
          .createNamedQuery("Plan.deactivateCheaperThan")
          .setParameter("minPrice", new BigDecimal("10.00"))
          .executeUpdate());
      System.out.println("rows updated = " + deactivated);
      plans(emf);

      step("5. executeUpdate() without a transaction");
      try (EntityManager em = emf.createEntityManager()) {
        em.createNativeQuery("update plan set active = true").executeUpdate();
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("6. A loaded entity keeps the old price; refresh() reloads it");
      reset(emf, ids);
      emf.runInTransaction(em -> {
        Plan premium = em.find(Plan.class, ids.get("Premium"));
        System.out.println("loaded: " + premium.getMonthlyPrice());
        em.createNativeQuery("update plan set monthly_price = :price where name = :name")
            .setParameter("price", new BigDecimal("21.99"))
            .setParameter("name", "Premium")
            .executeUpdate();
        System.out.println("after native update: " + premium.getMonthlyPrice());
        Plan again = em.find(Plan.class, ids.get("Premium"));
        System.out.println("em.find() again: " + again.getMonthlyPrice() + " (same object: " + (again == premium) + ")");
        em.refresh(premium);
        System.out.println("after refresh(): " + premium.getMonthlyPrice());
      });

      step("7. clear() detaches everything; the next find() reads the row");
      reset(emf, ids);
      emf.runInTransaction(em -> {
        Plan premium = em.find(Plan.class, ids.get("Premium"));
        em.createNativeQuery("update plan set monthly_price = 21.99 where name = 'Premium'").executeUpdate();
        em.clear();
        Plan fresh = em.find(Plan.class, ids.get("Premium"));
        System.out.println("after clear() and find(): " + fresh.getMonthlyPrice()
            + ", old object: " + premium.getMonthlyPrice() + ", managed: " + em.contains(premium));
      });

      step("8. Lost update: the stale entity is written back at commit");
      reset(emf, ids);
      emf.runInTransaction(em -> {
        Plan premium = em.find(Plan.class, ids.get("Premium"));
        em.createNativeQuery("update plan set monthly_price = 21.99 where name = 'Premium'").executeUpdate();
        premium.setName("Premium 4K");
      });
      System.out.println("price in table: " + Database.priceInTable(emf, "Premium 4K"));

      step("9. refresh() before the change keeps the native update");
      reset(emf, ids);
      emf.runInTransaction(em -> {
        Plan premium = em.find(Plan.class, ids.get("Premium"));
        em.createNativeQuery("update plan set monthly_price = 21.99 where name = 'Premium'").executeUpdate();
        em.refresh(premium);
        premium.setName("Premium 4K");
      });
      System.out.println("price in table: " + Database.priceInTable(emf, "Premium 4K"));

      step("10. Auto flush before a native update");
      reset(emf, ids);
      int flushed = emf.callInTransaction(em -> {
        em.find(Plan.class, ids.get("Standard")).setActive(false);
        return em.createNativeQuery("update plan set monthly_price = monthly_price + 1 where active = true")
            .executeUpdate();
      });
      System.out.println("rows updated = " + flushed);
      plans(emf);

      step("11. addSynchronizedEntityClass(Plan.class): the flush still happens");
      reset(emf, ids);
      int synced = emf.callInTransaction(em -> {
        em.find(Plan.class, ids.get("Standard")).setActive(false);
        return em.createNativeQuery("update plan set monthly_price = monthly_price + 1 where active = true")
            .unwrap(NativeQuery.class)
            .addSynchronizedEntityClass(Plan.class)
            .executeUpdate();
      });
      System.out.println("rows updated = " + synced);

      step("12. addSynchronizedQuerySpace(\"addon\"): wrong table, no flush");
      reset(emf, ids);
      int notFlushed = emf.callInTransaction(em -> {
        em.find(Plan.class, ids.get("Standard")).setActive(false);
        return em.createNativeQuery("update plan set monthly_price = monthly_price + 1 where active = true")
            .unwrap(NativeQuery.class)
            .addSynchronizedQuerySpace("addon")
            .executeUpdate();
      });
      System.out.println("rows updated = " + notFlushed);
      plans(emf);

      step("13. Second-level cache after a native update with no table declared");
      reset(emf, ids);
      warm(emf, ids);
      cache(emf, ids);
      emf.runInTransaction(em -> em.createNativeQuery("update plan set active = true where name = 'Legacy HD'")
          .executeUpdate());
      cache(emf, ids);

      step("14. Second-level cache after addSynchronizedEntityClass(Plan.class)");
      warm(emf, ids);
      cache(emf, ids);
      emf.runInTransaction(em -> em.createNativeQuery("update plan set active = false where name = 'Legacy HD'")
          .unwrap(NativeQuery.class)
          .addSynchronizedEntityClass(Plan.class)
          .executeUpdate());
      cache(emf, ids);

      step("15. Second-level cache after addSynchronizedQuerySpace(\"addon\") on a plan update");
      reset(emf, ids);
      warm(emf, ids);
      emf.runInTransaction(em -> em.createNativeQuery("update plan set monthly_price = 8.49 where name = 'Basic'")
          .unwrap(NativeQuery.class)
          .addSynchronizedQuerySpace("addon")
          .executeUpdate());
      cache(emf, ids);
      BigDecimal cachedBasic = emf.callInTransaction(em -> em.find(Plan.class, ids.get("Basic")).getMonthlyPrice());
      System.out.println("em.find() Basic: " + cachedBasic + ", in table: " + Database.priceInTable(emf, "Basic"));

      step("16. Named native query with the org.hibernate.query.native.spaces hint");
      reset(emf, ids);
      warm(emf, ids);
      int setPrice = emf.callInTransaction(em -> em.createNamedQuery("Plan.setPrice")
          .setParameter("price", new BigDecimal("9.49"))
          .setParameter("name", "Basic")
          .executeUpdate());
      System.out.println("rows updated = " + setPrice);
      cache(emf, ids);

      step("17. Query cache: cached add-on list after each kind of native update");
      reset(emf, ids);
      Statistics stats = Database.statistics(emf);
      addons(emf);
      emf.runInTransaction(em -> em.createNativeQuery("update plan set active = true where name = 'Legacy HD'")
          .executeUpdate());
      stats.clear();
      addons(emf);
      System.out.println("nothing declared: query cache hits = " + stats.getQueryCacheHitCount()
          + ", misses = " + stats.getQueryCacheMissCount());
      emf.runInTransaction(em -> em.createNativeQuery("update plan set active = false where name = 'Legacy HD'")
          .unwrap(NativeQuery.class)
          .addSynchronizedEntityClass(Plan.class)
          .executeUpdate());
      stats.clear();
      addons(emf);
      System.out.println("Plan declared: query cache hits = " + stats.getQueryCacheHitCount()
          + ", misses = " + stats.getQueryCacheMissCount());
      emf.runInTransaction(em -> em.createNativeQuery("update plan set active = true where name = 'Legacy HD'")
          .unwrap(NativeQuery.class)
          .addSynchronizedQuerySpace("addon")
          .executeUpdate());
      stats.clear();
      addons(emf);
      System.out.println("addon declared: query cache hits = " + stats.getQueryCacheHitCount()
          + ", misses = " + stats.getQueryCacheMissCount());

      step("18. Three ways to raise the active prices: dirty checking, JPQL, native SQL");
      reset(emf, ids);
      Plan.preUpdateCalls = 0;
      emf.runInTransaction(em -> em.createQuery("select p from Plan p where p.active = true", Plan.class)
          .getResultList()
          .forEach(p -> p.setMonthlyPrice(p.getMonthlyPrice().add(BigDecimal.ONE))));
      System.out.println("dirty checking: @PreUpdate calls = " + Plan.preUpdateCalls);
      Plan.preUpdateCalls = 0;
      int jpql = emf.callInTransaction(em -> em
          .createQuery("update Plan p set p.monthlyPrice = p.monthlyPrice + :amount where p.active = true")
          .setParameter("amount", BigDecimal.ONE)
          .executeUpdate());
      System.out.println("JPQL bulk update: rows = " + jpql + ", @PreUpdate calls = " + Plan.preUpdateCalls);
      int nativeRows = emf.callInTransaction(em -> em
          .createNativeQuery("update plan set monthly_price = monthly_price + :amount where active = true")
          .setParameter("amount", BigDecimal.ONE)
          .executeUpdate());
      System.out.println("native update: rows = " + nativeRows + ", @PreUpdate calls = " + Plan.preUpdateCalls);
      plans(emf);

      step("19. getResultList() on an UPDATE, executeUpdate() on a SELECT");
      try {
        emf.runInTransaction(em -> em.createNativeQuery("update plan set active = true").getResultList());
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
      try {
        emf.runInTransaction(em -> em.createNativeQuery("select * from plan").executeUpdate());
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }
  }

  /** Every case from step 6 on starts again from the four plans of step 1. */
  static void reset(EntityManagerFactory emf, Map<String, Long> ids) {
    System.out.println("(reset: empty the tables and the cache, save the four plans again)");
    ids.clear();
    ids.putAll(Database.reset(emf));
  }

  static void warm(EntityManagerFactory emf, Map<String, Long> ids) {
    emf.runInTransaction(em -> {
      em.find(Plan.class, ids.get("Basic"));
      em.find(Addon.class, ids.get("Sports"));
    });
  }

  static void cache(EntityManagerFactory emf, Map<String, Long> ids) {
    System.out.println("  second-level cache: Plan Basic = " + emf.getCache().contains(Plan.class, ids.get("Basic"))
        + ", Addon Sports = " + emf.getCache().contains(Addon.class, ids.get("Sports")));
  }

  static void addons(EntityManagerFactory emf) {
    emf.runInTransaction(em -> em.createQuery("select a from Addon a", Addon.class)
        .setHint("org.hibernate.cacheable", true)
        .getResultList());
  }

  static void plans(EntityManagerFactory emf) {
    List<Plan> all = emf.callInTransaction(em -> em
        .createQuery("select p from Plan p order by p.id", Plan.class)
        .setHint("jakarta.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS)
        .getResultList());
    System.out.println("  plans: " + all);
  }

  static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }
}
