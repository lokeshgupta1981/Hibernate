package com.howtodoinjava.hibernate.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;

public class HibernateSearchDemo {

  public static void main(String[] args) throws Exception {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Persist five episodes: Hibernate Search indexes them on commit");
      emf.runInTransaction(em -> {
        Database.episodes().forEach(em::persist);
        em.flush();
        System.out.println("   search before commit: " + EpisodeSearch.match(em, "testing"));
      });
      emf.runInTransaction(em ->
          System.out.println("   search after commit: " + EpisodeSearch.match(em, "testing")));

      step("2. SQL LIKE vs full-text match");
      emf.runInTransaction(em -> {
        System.out.println("   like '%testing%': " + EpisodeSearch.like(em, "testing"));
        System.out.println("   like '%test%':    " + EpisodeSearch.like(em, "test"));
        System.out.println("   match 'testing':  " + EpisodeSearch.match(em, "testing"));
      });

      step("3. match with boost, fuzzy and count");
      emf.runInTransaction(em -> {
        System.out.println("   match 'test code':      " + EpisodeSearch.match(em, "test code"));
        System.out.println("   boosted 'test code':    " + EpisodeSearch.matchBoosted(em, "test code"));
        System.out.println("   fuzzy 'virtul':         " + EpisodeSearch.fuzzy(em, "virtul"));
        System.out.println("   count 'threads':        " + EpisodeSearch.count(em, "threads"));
      });

      step("4. match vs phrase");
      emf.runInTransaction(em -> {
        System.out.println("   match 'virtual threads':  " + EpisodeSearch.match(em, "virtual threads"));
        System.out.println("   phrase 'virtual threads': " + EpisodeSearch.phrase(em, "virtual threads"));
      });

      step("5. bool: must, filter, mustNot");
      emf.runInTransaction(em -> {
        System.out.println("   'threads' by Anna:       " + EpisodeSearch.textByHost(em, "threads", "Anna"));
        System.out.println("   'test' not by Ravi:      " + EpisodeSearch.textWithoutHost(em, "test", "Ravi"));
      });

      step("6. range and sort");
      emf.runInTransaction(em -> {
        System.out.println("   30 to 45 minutes:        " + EpisodeSearch.durationBetween(em, 30, 45));
        System.out.println("   since 2026-03-01:        " + EpisodeSearch.publishedSince(em, LocalDate.of(2026, 3, 1)));
        System.out.println("   newest first:            " + EpisodeSearch.newestFirst(em));
      });

      step("7. pagination");
      emf.runInTransaction(em -> {
        SearchResult<Episode> page = EpisodeSearch.page(em, 1, 2);
        System.out.println("   page 2 (size 2): " + page.hits() + ", total hits: " + page.total().hitCount());
      });

      step("8. highlighting");
      emf.runInTransaction(em ->
          System.out.println("   " + EpisodeSearch.highlight(em, "tests")));

      step("9. keyword fields are exact");
      emf.runInTransaction(em -> {
        System.out.println("   host = 'Lokesh':            " + EpisodeSearch.byHost(em, "host", "Lokesh"));
        System.out.println("   host = 'lokesh':            " + EpisodeSearch.byHost(em, "host", "lokesh"));
        System.out.println("   host_ignorecase = 'lokesh': " + EpisodeSearch.byHost(em, "host_ignorecase", "lokesh"));
      });

      step("10. Update and delete keep the index in sync");
      emf.runInTransaction(em -> {
        Episode reviews = EpisodeSearch.match(em, "reviews").get(0);
        reviews.setTitle("Code Reviews and Testing");
      });
      emf.runInTransaction(em ->
          System.out.println("   after rename, 'testing': " + EpisodeSearch.match(em, "testing")));
      emf.runInTransaction(em -> em.remove(EpisodeSearch.match(em, "reviews").get(0)));
      emf.runInTransaction(em ->
          System.out.println("   after delete, 'testing': " + EpisodeSearch.match(em, "testing")));

      step("11. Rows written with SQL are not indexed until the MassIndexer runs");
      emf.runInTransaction(em -> em.createNativeQuery(
              "insert into Episode (id, title, description, host, publishedOn, durationMinutes) "
                  + "values (100, 'Kotlin for Java Developers', 'Null safety and data classes.', 'Ravi', DATE '2026-06-12', 39)")
          .executeUpdate());
      emf.runInTransaction(em ->
          System.out.println("   before mass indexing, 'kotlin': " + EpisodeSearch.match(em, "kotlin")));
      try (EntityManager em = emf.createEntityManager()) {
        Search.session(em).massIndexer(Episode.class).startAndWait();   // own threads and transactions
      }
      emf.runInTransaction(em ->
          System.out.println("   after mass indexing, 'kotlin':  " + EpisodeSearch.match(em, "kotlin")));
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }
}
