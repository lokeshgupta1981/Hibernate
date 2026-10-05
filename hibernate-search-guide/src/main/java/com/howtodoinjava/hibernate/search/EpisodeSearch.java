package com.howtodoinjava.hibernate.search;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

/** Every search shown in the article. Each method needs an open EntityManager. */
public final class EpisodeSearch {

  private EpisodeSearch() {
  }

  /** SQL LIKE for comparison. */
  public static List<Episode> like(EntityManager em, String text) {
    return em.createQuery("from Episode where lower(description) like :text order by id", Episode.class)
        .setParameter("text", "%" + text + "%")
        .getResultList();
  }

  /** match: full-text search in one or more fields, best match first. */
  public static List<Episode> match(EntityManager em, String text) {
    SearchSession searchSession = Search.session(em);
    return searchSession.search(Episode.class)
        .where(f -> f.match().fields("title", "description").matching(text))
        .fetchHits(20);
  }

  /** match with boost: a hit in the title counts twice. */
  public static List<Episode> matchBoosted(EntityManager em, String text) {
    return Search.session(em).search(Episode.class)
        .where(f -> f.match()
            .field("title").boost(2.0f)
            .field("description")
            .matching(text))
        .fetchHits(20);
  }

  /** match with fuzzy: tolerates one typo per word. */
  public static List<Episode> fuzzy(EntityManager em, String text) {
    return Search.session(em).search(Episode.class)
        .where(f -> f.match().fields("title", "description").matching(text).fuzzy(1))
        .fetchHits(20);
  }

  /** phrase: the words must appear next to each other, in this order. */
  public static List<Episode> phrase(EntityManager em, String text) {
    return Search.session(em).search(Episode.class)
        .where(f -> f.phrase().fields("title", "description").matching(text))
        .fetchHits(20);
  }

  /** bool: full-text condition plus an exact filter on the host. */
  public static List<Episode> textByHost(EntityManager em, String text, String host) {
    return Search.session(em).search(Episode.class)
        .where(f -> f.bool()
            .must(f.match().fields("title", "description").matching(text))
            .filter(f.match().field("host").matching(host)))
        .fetchHits(20);
  }

  /** bool with mustNot. */
  public static List<Episode> textWithoutHost(EntityManager em, String text, String host) {
    return Search.session(em).search(Episode.class)
        .where(f -> f.bool()
            .must(f.match().fields("title", "description").matching(text))
            .mustNot(f.match().field("host").matching(host)))
        .fetchHits(20);
  }

  /** range on a number. */
  public static List<Episode> durationBetween(EntityManager em, int min, int max) {
    return Search.session(em).search(Episode.class)
        .where(f -> f.range().field("durationMinutes").between(min, max))
        .sort(f -> f.field("durationMinutes"))
        .fetchHits(20);
  }

  /** range on a date. */
  public static List<Episode> publishedSince(EntityManager em, LocalDate date) {
    return Search.session(em).search(Episode.class)
        .where(f -> f.range().field("publishedOn").atLeast(date))
        .sort(f -> f.field("publishedOn"))
        .fetchHits(20);
  }

  /** sort: newest first instead of best match first. */
  public static List<Episode> newestFirst(EntityManager em) {
    return Search.session(em).search(Episode.class)
        .where(f -> f.matchAll())
        .sort(f -> f.field("publishedOn").desc())
        .fetchHits(20);
  }

  /** pagination: one page of results plus the total hit count. */
  public static SearchResult<Episode> page(EntityManager em, int pageNumber, int pageSize) {
    return Search.session(em).search(Episode.class)
        .where(f -> f.matchAll())
        .sort(f -> f.field("title_sort"))
        .fetch(pageNumber * pageSize, pageSize);
  }

  /** highlighting: fragments of the description with the matched words marked. */
  public static List<List<String>> highlight(EntityManager em, String text) {
    return Search.session(em).search(Episode.class)
        .select(f -> f.highlight("description"))
        .where(f -> f.match().field("description").matching(text))
        .fetchHits(20);
  }

  /** count only, no entities loaded. */
  public static long count(EntityManager em, String text) {
    return Search.session(em).search(Episode.class)
        .where(f -> f.match().fields("title", "description").matching(text))
        .fetchTotalHitCount();
  }

  /** keyword field: exact value. */
  public static List<Episode> byHost(EntityManager em, String field, String host) {
    return Search.session(em).search(Episode.class)
        .where(f -> f.match().field(field).matching(host))
        .sort(f -> f.field("publishedOn"))
        .fetchHits(20);
  }
}
