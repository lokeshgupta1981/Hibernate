package com.howtodoinjava.hibernate.lazy;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.function.Supplier;
import org.hibernate.Hibernate;
import org.hibernate.stat.Statistics;

public class LazyLoadingDemo {

  private static EntityManagerFactory emf;

  public static void main(String[] args) {
    try (EntityManagerFactory factory = Database.create(true)) {
      emf = factory;

      step("1. Save three restaurants with two menu items each");
      Long greenBowlId = Database.seed(emf);
      Long pastaId = emf.callInTransaction(em -> em.createQuery(
          "select m.id from MenuItem m where m.name = 'Pasta'", Long.class).getSingleResult());
      Long mondayId = emf.callInTransaction(em -> em.createQuery(
          "select d.id from DailySpecial d where d.weekday = 'Monday'", Long.class).getSingleResult());

      step("2. LAZY collection: find a restaurant, then read its menu");
      emf.runInTransaction(em -> {
        Restaurant restaurant = em.find(Restaurant.class, greenBowlId);
        List<MenuItem> menu = restaurant.getMenu();
        print("menu class", menu.getClass().getName());
        print("initialized", Hibernate.isInitialized(menu));
        print("loaded (JPA)", emf.getPersistenceUnitUtil().isLoaded(restaurant, "menu"));
        print("menu.size()", menu.size());
        print("initialized", Hibernate.isInitialized(menu));
      });

      step("3. LAZY @ManyToOne: find a menu item, then read its restaurant (proxy)");
      emf.runInTransaction(em -> {
        MenuItem pasta = em.find(MenuItem.class, pastaId);
        Restaurant restaurant = pasta.getRestaurant();
        print("restaurant class", restaurant.getClass().getName());
        print("is a Restaurant", restaurant instanceof Restaurant);
        print("initialized", Hibernate.isInitialized(restaurant));
        print("restaurant.getId()", restaurant.getId());
        print("restaurant.getName()", restaurant.getName());
        print("initialized", Hibernate.isInitialized(restaurant));
        print("Hibernate.getClass()", Hibernate.getClass(restaurant).getName());
      });

      step("3b. EAGER @ManyToOne: find a daily special");
      emf.runInTransaction(em -> {
        DailySpecial monday = em.find(DailySpecial.class, mondayId);
        print("item class", monday.getItem().getClass().getName());
        print("item", monday.getItem().getName());
      });

      step("4. LazyInitializationException: lazy collection after the transaction");
      Restaurant detached = emf.callInTransaction(em -> em.find(Restaurant.class, greenBowlId));
      error(() -> detached.getMenu().size());

      step("5. LazyInitializationException: proxy after the transaction");
      MenuItem detachedPasta = emf.callInTransaction(em -> em.find(MenuItem.class, pastaId));
      print("restaurant.getId()", detachedPasta.getRestaurant().getId());
      error(() -> detachedPasta.getRestaurant().getName());

      step("6. Fix: join fetch");
      Restaurant fetched = count(() -> emf.callInTransaction(em -> em.createQuery(
          "select r from Restaurant r join fetch r.menu where r.id = :id", Restaurant.class)
          .setParameter("id", greenBowlId)
          .getSingleResult()));
      print("after commit", fetched.getMenu());

      step("7. Fix: entity graph with find() (JPA 3.2)");
      Restaurant withGraph = count(() -> emf.callInTransaction(em -> {
        EntityGraph<Restaurant> graph = em.createEntityGraph(Restaurant.class);
        graph.addAttributeNode("menu");
        return em.find(graph, greenBowlId);
      }));
      print("after commit", withGraph.getMenu());

      step("8. Fix: named entity graph as a query hint");
      Restaurant named = count(() -> emf.callInTransaction(em -> em.createQuery(
          "select r from Restaurant r where r.id = :id", Restaurant.class)
          .setParameter("id", greenBowlId)
          .setHint("jakarta.persistence.fetchgraph", em.getEntityGraph("Restaurant.menu"))
          .getSingleResult()));
      print("after commit", named.getMenu());

      step("9. Fix: Hibernate.initialize() inside the transaction");
      Restaurant initialized = count(() -> emf.callInTransaction(em -> {
        Restaurant restaurant = em.find(Restaurant.class, greenBowlId);
        Hibernate.initialize(restaurant.getMenu());
        return restaurant;
      }));
      print("after commit", initialized.getMenu());

      step("10. Fix: DTO projection");
      List<MenuLine> lines = count(() -> emf.callInTransaction(em -> em.createQuery(
          "select r.name, m.name, m.price from MenuItem m join m.restaurant r "
              + "where r.id = :id order by m.name", MenuLine.class)
          .setParameter("id", greenBowlId)
          .getResultList()));
      print("lines", lines);

      step("11. N+1: list all restaurants and read each menu");
      count(() -> emf.callInTransaction(em -> {
        List<Restaurant> all = em.createQuery("select r from Restaurant r order by r.id", Restaurant.class)
            .getResultList();
        all.forEach(r -> print(r.getName(), r.getMenu().size() + " items"));
        return all.size();
      }));

      step("12. N+1 fixed with join fetch");
      count(() -> emf.callInTransaction(em -> {
        List<Restaurant> all = em.createQuery(
            "select distinct r from Restaurant r join fetch r.menu order by r.id", Restaurant.class)
            .getResultList();
        all.forEach(r -> print(r.getName(), r.getMenu().size() + " items"));
        return all.size();
      }));

      step("13. EAGER @ManyToOne in a query: one extra SELECT per menu item");
      count(() -> emf.callInTransaction(em ->
          em.createQuery("select d from DailySpecial d", DailySpecial.class).getResultList().size()));

      step("14. @Basic(fetch = LAZY) on MenuItem (not enhanced) and MenuCard (enhanced)");
      emf.runInTransaction(em -> {
        MenuItem pasta = em.find(MenuItem.class, pastaId);
        print("MenuItem description loaded", emf.getPersistenceUnitUtil().isLoaded(pasta, "description"));
      });
      emf.runInTransaction(em -> {
        MenuCard card = em.createQuery("select c from MenuCard c", MenuCard.class).getSingleResult();
        print("MenuCard text loaded", emf.getPersistenceUnitUtil().isLoaded(card, "text"));
        print("card.getText()", card.getText());
        print("MenuCard text loaded", emf.getPersistenceUnitUtil().isLoaded(card, "text"));
      });
    }

    step("15. hibernate.default_batch_fetch_size = 10: the N+1 loop again");
    try (EntityManagerFactory factory = Database.configuration(true)
        .jdbcUrl("jdbc:h2:mem:batchfetch;DB_CLOSE_DELAY=-1")
        .property("hibernate.default_batch_fetch_size", "10")
        .createEntityManagerFactory()) {
      emf = factory;
      Database.seed(emf);
      count(() -> emf.callInTransaction(em -> {
        List<Restaurant> all = em.createQuery("select r from Restaurant r order by r.id", Restaurant.class)
            .getResultList();
        all.forEach(r -> print(r.getName(), r.getMenu().size() + " items"));
        return all.size();
      }));
    }

    step("16. hibernate.enable_lazy_load_no_trans = true (anti-pattern)");
    try (EntityManagerFactory factory = Database.configuration(true)
        .jdbcUrl("jdbc:h2:mem:notrans;DB_CLOSE_DELAY=-1")
        .property("hibernate.enable_lazy_load_no_trans", "true")
        .createEntityManagerFactory()) {
      emf = factory;
      Long id = Database.seed(emf);
      Restaurant detached = emf.callInTransaction(em -> em.find(Restaurant.class, id));
      count(() -> {
        print("menu.size() after commit", detached.getMenu().size());
        return null;
      });
    }
  }

  private static <T> T count(Supplier<T> action) {
    Statistics statistics = Database.statistics(emf);
    T result = action.get();
    print("statements", statistics.getPrepareStatementCount());
    return result;
  }

  private static void error(Runnable action) {
    try {
      action.run();
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }

  private static void print(String label, Object value) {
    System.out.println("  -> " + label + ": " + value);
  }
}
