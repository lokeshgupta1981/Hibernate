package com.howtodoinjava.hibernate.firstlevelcache;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

public class FirstLevelCacheDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Save three movies");
      List<Long> ids = Database.saveMovies(emf);
      Long inceptionId = ids.get(0);
      Long upId = ids.get(1);

      step("2. Find the same movie twice in one EntityManager");
      SqlCounter.reset();
      emf.runInTransaction(em -> {
        Movie first = em.find(Movie.class, inceptionId);
        Movie second = em.find(Movie.class, inceptionId);
        print("first == second", first == second);
        print("em.contains(first)", em.contains(first));
      });
      print("select statements", SqlCounter.selects());

      step("3. Find the same movie in two EntityManagers");
      SqlCounter.reset();
      try (EntityManager em1 = emf.createEntityManager();
           EntityManager em2 = emf.createEntityManager()) {
        Movie a = em1.find(Movie.class, inceptionId);
        Movie b = em2.find(Movie.class, inceptionId);
        print("a == b", a == b);
        print("em2.contains(a)", em2.contains(a));
      }
      print("select statements", SqlCounter.selects());

      step("4. persist() puts the new movie in the cache");
      SqlCounter.reset();
      emf.runInTransaction(em -> {
        Movie soul = new Movie("Soul", 2020, 8.0);
        em.persist(soul);
        Movie found = em.find(Movie.class, soul.getId());
        print("found == soul", found == soul);
        print("statements before commit", SqlCounter.statements());
      });

      step("5. A JPQL query always runs SQL");
      SqlCounter.reset();
      emf.runInTransaction(em -> {
        Movie found = em.find(Movie.class, inceptionId);
        Movie queried = em.createQuery("from Movie where title = :title", Movie.class)
            .setParameter("title", "Inception")
            .getSingleResult();
        print("queried == found", queried == found);
        em.createQuery("from Movie where title = :title", Movie.class)
            .setParameter("title", "Inception")
            .getSingleResult();
        print("select statements after find + 2 queries", SqlCounter.selects());

        SqlCounter.reset();
        List<Movie> all = em.createQuery("from Movie order by id", Movie.class).getResultList();
        Movie up = em.find(Movie.class, upId);
        print("all", all);
        print("up taken from the query result", all.contains(up));
        print("select statements for query + find", SqlCounter.selects());
      });

      step("6. A bulk update does not change the cached movie; refresh() does");
      SqlCounter.reset();
      emf.runInTransaction(em -> {
        Movie inception = em.find(Movie.class, inceptionId);
        int rows = em.createQuery("update Movie set rating = 9.0 where id = :id")
            .setParameter("id", inceptionId)
            .executeUpdate();
        print("rows updated", rows);
        print("em.find(...).getRating()", em.find(Movie.class, inceptionId).getRating());
        Movie queried = em.createQuery("from Movie where id = :id", Movie.class)
            .setParameter("id", inceptionId)
            .getSingleResult();
        print("query result rating", queried.getRating());
        em.refresh(inception);
        print("after refresh()", inception.getRating());
      });
      emf.runInTransaction(em -> em.find(Movie.class, inceptionId).setRating(8.8));

      step("7. detach(), evict() and clear()");
      SqlCounter.reset();
      emf.runInTransaction(em -> {
        Session session = em.unwrap(Session.class);
        Movie inception = em.find(Movie.class, inceptionId);
        Movie up = em.find(Movie.class, upId);
        print("entities in context", session.getStatistics().getEntityCount());

        em.detach(inception);
        print("after detach: contains", em.contains(inception));
        Movie again = em.find(Movie.class, inceptionId);
        print("again == inception", again == inception);

        session.evict(up);
        print("after evict: contains", em.contains(up));

        em.clear();
        print("after clear: entities in context", session.getStatistics().getEntityCount());
        print("after clear: contains(again)", em.contains(again));
      });
      print("select statements", SqlCounter.selects());

      step("8. clear() drops unsaved changes; flush() first");
      SqlCounter.reset();
      emf.runInTransaction(em -> {
        em.find(Movie.class, upId).setTitle("Up!");
        em.clear();
      });
      print("title after clear()", title(emf, upId));
      emf.runInTransaction(em -> {
        em.find(Movie.class, upId).setTitle("Up!");
        em.flush();
        em.clear();
      });
      print("title after flush() + clear()", title(emf, upId));
      emf.runInTransaction(em -> em.find(Movie.class, upId).setTitle("Up"));

      step("9. Memory growth: 1000 inserts in one transaction");
      try (EntityManagerFactory quiet = Database.create(false, "bulk")) {
        print("most entities in context without clear()", insertMovies(quiet, 1000, 0));
        print("most entities in context with clear() every 50", insertMovies(quiet, 1000, 50));
      }

      step("10. getReference() returns the cached instance");
      SqlCounter.reset();
      emf.runInTransaction(em -> {
        Movie found = em.find(Movie.class, inceptionId);
        Movie ref = em.getReference(Movie.class, inceptionId);
        print("ref == found", ref == found);
      });
      print("select statements", SqlCounter.selects());

      step("11. StatelessSession has no first level cache");
      SqlCounter.reset();
      SessionFactory sf = emf.unwrap(SessionFactory.class);
      try (StatelessSession stateless = sf.openStatelessSession()) {
        Movie a = stateless.get(Movie.class, inceptionId);
        Movie b = stateless.get(Movie.class, inceptionId);
        print("a == b", a == b);
      }
      print("select statements", SqlCounter.selects());

      step("12. Statistics: entity loads vs find() calls");
      sf.getStatistics().clear();
      emf.runInTransaction(em -> {
        for (int i = 0; i < 5; i++) {
          em.find(Movie.class, inceptionId);
        }
      });
      print("entity load count for 5 find() calls", sf.getStatistics().getEntityLoadCount());
      print("prepared statements", sf.getStatistics().getPrepareStatementCount());
    }
  }

  /** Persists count movies and returns the largest number of entities the context held. */
  static int insertMovies(EntityManagerFactory emf, int count, int clearEvery) {
    return emf.callInTransaction(em -> {
      Session session = em.unwrap(Session.class);
      int max = 0;
      for (int i = 1; i <= count; i++) {
        em.persist(new Movie("Movie " + i, 2000 + i % 25, 7.0));
        max = Math.max(max, session.getStatistics().getEntityCount());
        if (clearEvery > 0 && i % clearEvery == 0) {
          em.flush();
          em.clear();
        }
      }
      return max;
    });
  }

  static String title(EntityManagerFactory emf, Long id) {
    return emf.callInTransaction(em -> em.find(Movie.class, id).getTitle());
  }

  static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }

  static void print(String label, Object value) {
    System.out.println(label + ": " + value);
  }
}
