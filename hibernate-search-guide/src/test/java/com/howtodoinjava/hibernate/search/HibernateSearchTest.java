package com.howtodoinjava.hibernate.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HibernateSearchTest {

  private EntityManagerFactory emf;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    emf.runInTransaction(em -> Database.episodes().forEach(em::persist));
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private static List<String> titles(List<Episode> episodes) {
    return episodes.stream().map(Episode::getTitle).toList();
  }

  private List<String> search(java.util.function.Function<EntityManager, List<Episode>> query) {
    return emf.callInTransaction(em -> titles(query.apply(em)));
  }

  @Test
  void indexIsUpdatedOnCommitNotBefore() {
    emf.runInTransaction(em -> {
      em.persist(new Episode("Kotlin Basics", "Null safety.", "Ravi", LocalDate.of(2026, 6, 1), 20));
      em.flush();
      assertEquals(List.of(), titles(EpisodeSearch.match(em, "kotlin")));
    });
    assertEquals(List.of("Kotlin Basics"), search(em -> EpisodeSearch.match(em, "kotlin")));
  }

  @Test
  void likeVersusFullText() {
    assertEquals(List.of(), search(em -> EpisodeSearch.like(em, "testing")));
    assertEquals(List.of("Testing Spring Boot Apps", "Hibernate Performance", "Code Reviews"),
        search(em -> EpisodeSearch.like(em, "test")));
    assertEquals(List.of("Testing Spring Boot Apps", "Code Reviews"),
        search(em -> EpisodeSearch.match(em, "testing")));
    assertEquals(List.of("Testing Spring Boot Apps", "Code Reviews"),
        search(em -> EpisodeSearch.match(em, "TESTS")));
  }

  @Test
  void boostChangesOrder() {
    assertEquals(List.of("Testing Spring Boot Apps", "Code Reviews", "Virtual Threads in Java 25"),
        search(em -> EpisodeSearch.match(em, "test code")));
    assertEquals(List.of("Code Reviews", "Testing Spring Boot Apps", "Virtual Threads in Java 25"),
        search(em -> EpisodeSearch.matchBoosted(em, "test code")));
  }

  @Test
  void fuzzyAndCount() {
    assertEquals(List.of(), search(em -> EpisodeSearch.match(em, "virtul")));
    assertEquals(List.of("Virtual Threads in Java 25"), search(em -> EpisodeSearch.fuzzy(em, "virtul")));
    long count = emf.callInTransaction(em -> EpisodeSearch.count(em, "threads"));
    assertEquals(2L, count);
  }

  @Test
  void matchVersusPhrase() {
    assertEquals(List.of("Virtual Threads in Java 25", "Hibernate Performance"),
        search(em -> EpisodeSearch.match(em, "virtual threads")));
    assertEquals(List.of("Virtual Threads in Java 25"),
        search(em -> EpisodeSearch.phrase(em, "virtual threads")));
  }

  @Test
  void boolPredicate() {
    assertEquals(List.of("Virtual Threads in Java 25"),
        search(em -> EpisodeSearch.textByHost(em, "threads", "Anna")));
    assertEquals(List.of("Testing Spring Boot Apps"),
        search(em -> EpisodeSearch.textWithoutHost(em, "test", "Ravi")));
  }

  @Test
  void rangeAndSort() {
    assertEquals(List.of("Records and Pattern Matching", "Virtual Threads in Java 25", "Testing Spring Boot Apps"),
        search(em -> EpisodeSearch.durationBetween(em, 30, 45)));
    assertEquals(List.of("Hibernate Performance", "Code Reviews", "Records and Pattern Matching"),
        search(em -> EpisodeSearch.publishedSince(em, LocalDate.of(2026, 3, 1))));
    assertEquals(List.of("Records and Pattern Matching", "Code Reviews", "Hibernate Performance",
            "Virtual Threads in Java 25", "Testing Spring Boot Apps"),
        search(EpisodeSearch::newestFirst));
  }

  @Test
  void pagination() {
    emf.runInTransaction(em -> {
      SearchResult<Episode> first = EpisodeSearch.page(em, 0, 2);
      assertEquals(List.of("Code Reviews", "Hibernate Performance"), titles(first.hits()));
      SearchResult<Episode> second = EpisodeSearch.page(em, 1, 2);
      assertEquals(List.of("Records and Pattern Matching", "Testing Spring Boot Apps"), titles(second.hits()));
      assertEquals(5L, second.total().hitCount());
      assertEquals(List.of("Virtual Threads in Java 25"), titles(EpisodeSearch.page(em, 2, 2).hits()));
    });
  }

  @Test
  void highlighting() {
    List<List<String>> fragments = emf.callInTransaction(em -> EpisodeSearch.highlight(em, "tests"));
    assertEquals(List.of(
        List.of("We write unit <em>tests</em> and integration <em>tests</em> for a Spring Boot service."),
        List.of("What we look for in a pull request, and how to <em>test</em> a change before merging.")),
        fragments);
  }

  @Test
  void highlightingNeedsHighlightableField() {
    SearchException e = assertThrows(SearchException.class, () -> emf.runInTransaction(em ->
        Search.session(em).search(Episode.class)
            .select(f -> f.highlight("title"))
            .where(f -> f.match().field("title").matching("testing"))
            .fetchHits(20)));
    System.out.println("HIGHLIGHT ERROR: " + e.getMessage());
    assertTrue(e.getMessage().contains("title"));
  }

  @Test
  void sortNeedsSortableField() {
    SearchException e = assertThrows(SearchException.class, () -> emf.runInTransaction(em ->
        Search.session(em).search(Episode.class)
            .where(f -> f.matchAll())
            .sort(f -> f.field("title"))
            .fetchHits(20)));
    System.out.println("SORT ERROR: " + e.getMessage());
    assertTrue(e.getMessage().contains("title"));
  }

  @Test
  void keywordFieldIsExact() {
    assertEquals(List.of("Testing Spring Boot Apps", "Hibernate Performance"),
        search(em -> EpisodeSearch.byHost(em, "host", "Lokesh")));
    assertEquals(List.of(), search(em -> EpisodeSearch.byHost(em, "host", "lokesh")));
    assertEquals(List.of("Testing Spring Boot Apps", "Hibernate Performance"),
        search(em -> EpisodeSearch.byHost(em, "host_ignorecase", "lokesh")));
  }

  @Test
  void updateAndDeleteAreIndexed() {
    emf.runInTransaction(em -> EpisodeSearch.match(em, "reviews").get(0).setTitle("Code Reviews and Testing"));
    assertEquals(List.of("Testing Spring Boot Apps", "Code Reviews and Testing"),
        search(em -> EpisodeSearch.match(em, "testing")));
    emf.runInTransaction(em -> em.remove(EpisodeSearch.match(em, "reviews").get(0)));
    assertEquals(List.of("Testing Spring Boot Apps"), search(em -> EpisodeSearch.match(em, "testing")));
  }

  @Test
  void jpqlBulkUpdateIsNotIndexed() {
    emf.runInTransaction(em -> em.createQuery("update Episode set title = 'Kotlin Reviews' where host = 'Ravi'")
        .executeUpdate());
    assertEquals(List.of(), search(em -> EpisodeSearch.match(em, "kotlin")));
    // the old title is still in the index; the entity loaded from the database has the new title
    assertEquals(List.of("Kotlin Reviews"), search(em -> EpisodeSearch.match(em, "code")).subList(0, 1));
  }

  @Test
  void massIndexerIndexesExistingRows() throws InterruptedException {
    emf.runInTransaction(em -> em.createNativeQuery(
            "insert into Episode (id, title, description, host, publishedOn, durationMinutes) "
                + "values (100, 'Kotlin for Java Developers', 'Null safety and data classes.', 'Ravi', DATE '2026-06-12', 39)")
        .executeUpdate());
    assertEquals(List.of(), search(em -> EpisodeSearch.match(em, "kotlin")));
    try (EntityManager em = emf.createEntityManager()) {
      Search.session(em).massIndexer(Episode.class).startAndWait();
    }
    assertEquals(List.of("Kotlin for Java Developers"), search(em -> EpisodeSearch.match(em, "kotlin")));
    long total = emf.callInTransaction(em -> Search.session(em).search(Episode.class)
        .where(f -> f.matchAll()).fetchTotalHitCount());
    assertEquals(6L, total);
  }

  @Test
  void indexingPlanReindexesOneEntity() {
    emf.runInTransaction(em -> em.createNativeQuery(
            "insert into Episode (id, title, description, host, publishedOn, durationMinutes) "
                + "values (101, 'Kotlin Coroutines', 'Suspend functions.', 'Anna', DATE '2026-07-01', 33)")
        .executeUpdate());
    emf.runInTransaction(em -> {
      Episode coroutines = em.find(Episode.class, 101L);
      Search.session(em).indexingPlan().addOrUpdate(coroutines);
    });
    assertEquals(List.of("Kotlin Coroutines"), search(em -> EpisodeSearch.match(em, "kotlin")));
  }

  @Test
  void hitsAreLoadedWithOneSelectButCountAndHighlightRunNoSql() {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.setStatisticsEnabled(true);

    stats.clear();
    emf.runInTransaction(em -> EpisodeSearch.match(em, "testing"));
    assertEquals(1L, stats.getPrepareStatementCount());   // select ... where id in (?,?)

    stats.clear();
    emf.runInTransaction(em -> EpisodeSearch.count(em, "testing"));
    assertEquals(0L, stats.getPrepareStatementCount());

    stats.clear();
    emf.runInTransaction(em -> EpisodeSearch.highlight(em, "tests"));
    assertEquals(0L, stats.getPrepareStatementCount());
  }
}
