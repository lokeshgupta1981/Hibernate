package com.howtodoinjava.hibernate.hql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Asserts every result, SQL statement and error message shown in the article. */
class HqlTest {

  private EntityManagerFactory emf;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    Database.seed(emf);
    SqlLog.clear();
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private List<String> titles(List<Artwork> artworks) {
    return artworks.stream().map(Artwork::getTitle).toList();
  }

  private List<List<Object>> rows(List<Object[]> rows) {
    return rows.stream().map(r -> java.util.Arrays.asList(r)).toList();
  }

  // ---- basics ------------------------------------------------------------

  @Test
  void selectWithWhereAndOrderBy() {
    List<Artwork> recent = emf.callInTransaction(em -> em.createQuery(
        "select a from Artwork a where a.year > 2019 order by a.year", Artwork.class).getResultList());
    assertEquals(List.of("Red Fields", "Old Bridge", "Night Train"), titles(recent));
    assertTrue(SqlLog.last().contains("from artworks a1_0 where a1_0.created_year>2019 order by a1_0.created_year"));
  }

  @Test
  void hqlAllowsOmittingSelect() {
    List<Artwork> recent = emf.callInTransaction(em -> em.createQuery(
        "from Artwork where year > 2019 order by year", Artwork.class).getResultList());
    assertEquals(List.of("Red Fields", "Old Bridge", "Night Train"), titles(recent));
  }

  @Test
  void strictJpqlModeRejectsHqlOnlySyntax() {
    emf.close();
    emf = Database.createJpqlOnly();
    Database.seed(emf);
    emf.runInTransaction(em -> {
      // Jakarta Persistence 3.2 made the select clause optional and added union
      assertEquals(3, em.createQuery("from Artwork where year > 2019", Artwork.class).getResultList().size());
      em.createQuery("select ar.name from Artist ar union select g.name from Gallery g", String.class).getResultList();
    });
    IllegalArgumentException limit = assertThrows(IllegalArgumentException.class, () -> emf.runInTransaction(em ->
        em.createQuery("select a.title from Artwork a order by a.title limit 2", String.class).getResultList()));
    assertTrue(limit.getMessage().contains(
        "Strict JPA query language compliance was violated: use of LIMIT/OFFSET clause"), limit.getMessage());
    IllegalArgumentException cte = assertThrows(IllegalArgumentException.class, () -> emf.runInTransaction(em ->
        em.createQuery("with e as (select a.title as title from Artwork a) select e.title from e", String.class)
            .getResultList()));
    assertTrue(cte.getMessage().contains("use of CTEs (common table expressions)"), cte.getMessage());
  }

  @Test
  void tableNameIsNotAnEntityName() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> emf.runInTransaction(em ->
        em.createQuery("select a from artwork a", Artwork.class).getResultList()));
    assertTrue(e.getMessage().contains("Could not resolve root entity 'artwork'"), e.getMessage());
  }

  @Test
  void selectionQueryWithNamedParameter() {
    List<Artwork> sculptures = emf.callInTransaction(em -> em.unwrap(Session.class).createSelectionQuery(
            "from Artwork a where a.medium = :medium order by a.title", Artwork.class)
        .setParameter("medium", Medium.SCULPTURE)
        .getResultList());
    assertEquals(List.of("Iron Leaf", "Stone Bird"), titles(sculptures));
    assertTrue(SqlLog.last().endsWith("where a1_0.medium=? order by a1_0.title"));
  }

  @Test
  void positionalParameters() {
    List<Artwork> range = emf.callInTransaction(em -> em.createQuery("""
            select a from Artwork a
            where a.estimatedValue between ?1 and ?2
            order by a.estimatedValue desc""", Artwork.class)
        .setParameter(1, new BigDecimal("800"))
        .setParameter(2, new BigDecimal("2500"))
        .getResultList());
    assertEquals(List.of("Iron Leaf", "Blue River", "Red Fields", "Old Bridge"), titles(range));
    assertTrue(SqlLog.last().contains("where a1_0.estimatedValue between ? and ? order by a1_0.estimatedValue desc"));
  }

  @Test
  void singleResultAndSingleResultOrNull() {
    emf.runInTransaction(em -> {
      Artwork bird = em.createQuery("from Artwork a where a.title = :title", Artwork.class)
          .setParameter("title", "Stone Bird").getSingleResult();
      Artwork none = em.createQuery("from Artwork a where a.title = :title", Artwork.class)
          .setParameter("title", "Sunflowers").getSingleResultOrNull();
      assertEquals("Stone Bird", bird.getTitle());
      assertNull(none);
      assertThrows(jakarta.persistence.NoResultException.class, () ->
          em.createQuery("from Artwork a where a.title = :title", Artwork.class)
              .setParameter("title", "Sunflowers").getSingleResult());
    });
  }

  @Test
  void parametersBlockInjection() {
    String input = "x' or '1'='1";
    emf.runInTransaction(em -> {
      List<Artwork> concatenated = em.createQuery(
          "from Artwork a where a.title = '" + input + "'", Artwork.class).getResultList();
      List<Artwork> bound = em.createQuery("from Artwork a where a.title = :title", Artwork.class)
          .setParameter("title", input).getResultList();
      assertEquals(6, concatenated.size());
      assertEquals(0, bound.size());
    });
  }

  // ---- projections -------------------------------------------------------

  @Test
  void projections() {
    emf.runInTransaction(em -> {
      List<String> titles = em.createQuery(
          "select a.title from Artwork a order by a.title", String.class).getResultList();
      assertEquals(List.of("Blue River", "Iron Leaf", "Night Train", "Old Bridge", "Red Fields", "Stone Bird"), titles);
      assertEquals("select a1_0.title from artworks a1_0 order by a1_0.title", SqlLog.last());

      List<Object[]> rows = em.createQuery(
              "select a.title, a.year from Artwork a where a.medium = :m order by a.year", Object[].class)
          .setParameter("m", Medium.SCULPTURE).getResultList();
      assertEquals(List.of(List.of("Stone Bird", 2015), List.of("Iron Leaf", 2018)), rows(rows));

      Tuple t = em.createQuery(
              "select a.title as title, a.estimatedValue as value from Artwork a where a.year = 2015", Tuple.class)
          .getSingleResult();
      assertEquals("Stone Bird", t.get("title", String.class));
      assertEquals(new BigDecimal("3000.00"), t.get("value", BigDecimal.class));
    });
  }

  @Test
  void recordWithSelectNewAndWithout() {
    List<ArtworkSummary> expected = List.of(new ArtworkSummary("Blue River", "Lokesh"),
        new ArtworkSummary("Old Bridge", "Emma"), new ArtworkSummary("Red Fields", "Hugo"));
    emf.runInTransaction(em -> {
      List<ArtworkSummary> withNew = em.createQuery("""
              select new com.howtodoinjava.hibernate.hql.ArtworkSummary(a.title, a.artist.name)
              from Artwork a where a.medium = :m order by a.title""", ArtworkSummary.class)
          .setParameter("m", Medium.PAINTING).getResultList();
      String sqlNew = SqlLog.last();
      List<ArtworkSummary> implicit = em.createQuery("""
              select a.title, a.artist.name
              from Artwork a where a.medium = :m order by a.title""", ArtworkSummary.class)
          .setParameter("m", Medium.PAINTING).getResultList();
      assertEquals(expected, withNew);
      assertEquals(expected, implicit);
      assertEquals(sqlNew, SqlLog.last());
      assertTrue(sqlNew.contains("join artists a2_0 on a2_0.id=a1_0.artist_id"));
    });
  }

  // ---- joins -------------------------------------------------------------

  @Test
  void implicitJoinThroughPath() {
    List<String> french = emf.callInTransaction(em -> em.createQuery(
        "select a.title from Artwork a where a.artist.country = :country order by a.title", String.class)
            .setParameter("country", "France")
        .getResultList());
    assertEquals(List.of("Iron Leaf", "Old Bridge", "Red Fields", "Stone Bird"), french);
    assertEquals("select a1_0.title from artworks a1_0 join artists a2_0 on a2_0.id=a1_0.artist_id "
        + "where a2_0.country=? order by a1_0.title", SqlLog.last());
  }

  @Test
  void implicitJoinDropsArtworkWithoutGallery() {
    List<String> titles = emf.callInTransaction(em -> em.createQuery(
        "select a.title from Artwork a order by a.gallery.name, a.title", String.class).getResultList());
    assertEquals(List.of("Blue River", "Night Train", "Old Bridge", "Iron Leaf", "Stone Bird"), titles);
    assertTrue(SqlLog.last().contains("from artworks a1_0 join galleries g1_0"));
  }

  @Test
  void innerVersusLeftJoin() {
    emf.runInTransaction(em -> {
      List<Object[]> inner = em.createQuery("""
          select g.name, a.title from Gallery g join g.artworks a
          order by g.name, a.title""", Object[].class).getResultList();
      assertEquals(5, inner.size());
      assertEquals(List.of("Modern Wing", "Blue River"), rows(inner).getFirst());
      assertTrue(SqlLog.last().contains("from galleries g1_0 join artworks a1_0 on g1_0.id=a1_0.gallery_id"));

      List<Object[]> left = em.createQuery("""
          select g.name, a.title from Gallery g left join g.artworks a
          order by g.name, a.title""", Object[].class).getResultList();
      assertEquals(6, left.size());
      assertEquals(java.util.Arrays.asList("East Room", null), rows(left).getFirst());
      assertTrue(SqlLog.last().contains("from galleries g1_0 left join artworks a1_0 on g1_0.id=a1_0.gallery_id"));
    });
  }

  @Test
  void leftJoinWithOnCondition() {
    List<Object[]> rows = emf.callInTransaction(em -> em.createQuery("""
            select ar.name, a.title from Artist ar
            left join ar.artworks a on a.medium = :m
            order by ar.name""", Object[].class)
        .setParameter("m", Medium.SCULPTURE).getResultList());
    assertEquals(List.of(List.of("Emma", "Iron Leaf"), List.of("Hugo", "Stone Bird"),
        java.util.Arrays.asList("Lokesh", null), java.util.Arrays.asList("Mia", null)), rows(rows));
    assertTrue(SqlLog.last().contains("left join artworks a2_0 on a1_0.id=a2_0.artist_id and a2_0.medium=?"));
  }

  @Test
  void lazyCollectionRunsOneQueryPerArtist() {
    emf.runInTransaction(em -> {
      List<Artist> artists = em.createQuery("from Artist ar order by ar.name", Artist.class).getResultList();
      artists.forEach(ar -> ar.getArtworks().size());
    });
    assertEquals(5, SqlLog.statements().size());
  }

  @Test
  void joinFetchLoadsArtworksInOneQuery() {
    emf.runInTransaction(em -> {
      List<Artist> artists = em.createQuery(
          "select ar from Artist ar left join fetch ar.artworks order by ar.name", Artist.class).getResultList();
      assertEquals(4, artists.size());          // no duplicates without distinct
      assertEquals(List.of("Old Bridge", "Iron Leaf"),
          artists.getFirst().getArtworks().stream().map(Artwork::getTitle).toList());
      assertEquals(0, artists.getLast().getArtworks().size());
    });
    assertEquals(1, SqlLog.statements().size());
    assertTrue(SqlLog.last().contains("from artists a1_0 left join artworks a2_0 on a1_0.id=a2_0.artist_id"));
  }

  // ---- aggregates, subqueries, expressions --------------------------------

  @Test
  void groupByHaving() {
    List<Object[]> rows = emf.callInTransaction(em -> em.createQuery("""
        select g.name, count(a), sum(a.estimatedValue)
        from Gallery g join g.artworks a
        group by g.name
        having count(a) > 1
        order by g.name""", Object[].class).getResultList());
    assertEquals(List.of(List.of("Modern Wing", 3L, new BigDecimal("2450.00")),
        List.of("Sculpture Hall", 2L, new BigDecimal("5200.00"))), rows(rows));
    assertTrue(SqlLog.last().contains("group by g1_0.name having count(a1_0.id)>1"));
  }

  @Test
  void subqueryAboveAverage() {
    List<String> titles = emf.callInTransaction(em -> em.createQuery("""
        select a.title from Artwork a
        where a.estimatedValue > (select avg(a2.estimatedValue) from Artwork a2)
        order by a.title""", String.class).getResultList());
    assertEquals(List.of("Iron Leaf", "Stone Bird"), titles);
  }

  @Test
  void existsAndNotExists() {
    emf.runInTransaction(em -> {
      List<String> sculptors = em.createQuery("""
              select ar.name from Artist ar
              where exists (select 1 from Artwork a where a.artist = ar and a.medium = :m)
              order by ar.name""", String.class)
          .setParameter("m", Medium.SCULPTURE).getResultList();
      assertEquals(List.of("Emma", "Hugo"), sculptors);
      assertTrue(SqlLog.last().contains("where exists(select 1 from artworks a2_0 where a2_0.artist_id=a1_0.id and a2_0.medium=?)"));
      List<String> noWork = em.createQuery("""
          select ar.name from Artist ar
          where not exists (select 1 from Artwork a where a.artist = ar)""", String.class).getResultList();
      assertEquals(List.of("Mia"), noWork);
    });
  }

  @Test
  void caseAndCoalesce() {
    List<Object[]> rows = emf.callInTransaction(em -> em.createQuery("""
        select a.title,
               case when a.estimatedValue >= 2000 then 'high' else 'normal' end,
               coalesce(g.name, 'Storage')
        from Artwork a left join a.gallery g
        order by a.title""", Object[].class).getResultList());
    assertEquals(6, rows.size());
    assertEquals(List.of("Iron Leaf", "high", "Sculpture Hall"), rows(rows).get(1));
    assertEquals(List.of("Red Fields", "normal", "Storage"), rows(rows).get(4));
  }

  @Test
  void stringFunctions() {
    List<Object[]> rows = emf.callInTransaction(em -> em.createQuery("""
        select upper(ar.name), ar.name || ' (' || ar.country || ')', length(ar.name)
        from Artist ar where ar.country = 'France' order by ar.name""", Object[].class).getResultList());
    assertEquals(List.of(List.of("EMMA", "Emma (France)", 4), List.of("HUGO", "Hugo (France)", 4)), rows(rows));
    assertTrue(SqlLog.last().contains("character_length(a1_0.name)"));
  }

  // ---- pagination and Hibernate 6/7 syntax -------------------------------

  @Test
  void paginationWithSetFirstResult() {
    List<String> page = emf.callInTransaction(em -> em.createQuery(
            "select a.title from Artwork a order by a.title", String.class)
        .setFirstResult(2).setMaxResults(2).getResultList());
    assertEquals(List.of("Night Train", "Old Bridge"), page);
    assertTrue(SqlLog.last().endsWith("order by a1_0.title offset ? rows fetch first ? rows only"));
  }

  @Test
  void limitOffsetInHql() {
    List<String> page = emf.callInTransaction(em -> em.createQuery(
        "select a.title from Artwork a order by a.title limit 2 offset 2", String.class).getResultList());
    assertEquals(List.of("Night Train", "Old Bridge"), page);
    assertTrue(SqlLog.last().endsWith("order by a1_0.title offset 2 rows fetch first 2 rows only"));
  }

  @Test
  void union() {
    List<String> names = emf.callInTransaction(em -> em.createQuery("""
        select ar.name from Artist ar where ar.country = 'France'
        union
        select g.name from Gallery g where g.floor = 0""", String.class).getResultList());
    assertEquals(List.of("Emma", "Hugo", "Sculpture Hall"), names.stream().sorted().toList());
    assertTrue(SqlLog.last().contains(" union select g1_0.name from galleries g1_0 where g1_0.floor=0"));
  }

  @Test
  void fromLessSelect() {
    Object value = emf.callInTransaction(em -> em.createQuery("select 2 + 3").getSingleResult());
    assertEquals(5, value);
    assertEquals("select (2+3)", SqlLog.last());
  }

  @Test
  void commonTableExpression() {
    List<String> titles = emf.callInTransaction(em -> em.createQuery("""
        with expensive as (
          select a.title as title, a.estimatedValue as price
          from Artwork a where a.estimatedValue > 1000
        )
        select e.title from expensive e order by e.price desc""", String.class).getResultList());
    assertEquals(List.of("Stone Bird", "Iron Leaf", "Blue River"), titles);
    // H2: Hibernate renders the CTE as a derived table in the from clause
    assertTrue(SqlLog.last().startsWith("select e1_0.title from (select a1_0.title,a1_0.estimatedValue from artworks a1_0"));
  }

  // ---- update, delete, insert --------------------------------------------

  @Test
  void bulkUpdateSkipsLoadedEntities() {
    emf.runInTransaction(em -> {
      Artwork train = em.createQuery("from Artwork a where a.title = 'Night Train'", Artwork.class)
          .getSingleResult();
      int updated = em.unwrap(Session.class).createMutationQuery(
              "update Artwork a set a.estimatedValue = a.estimatedValue * 1.1 where a.medium = :m")
          .setParameter("m", Medium.PHOTO)
          .executeUpdate();
      assertEquals(1, updated);
      assertEquals("update artworks a1_0 set estimatedValue=(a1_0.estimatedValue*1.1) where a1_0.medium=?",
          SqlLog.last());
      assertEquals(new BigDecimal("450.00"), train.getEstimatedValue());     // stale
      em.refresh(train);
      assertEquals(new BigDecimal("495.00"), train.getEstimatedValue());
    });
  }

  @Test
  void jpaExecuteUpdateWorksToo() {
    int deleted = emf.callInTransaction(em ->
        em.createQuery("delete from Artwork a where a.gallery is null").executeUpdate());
    assertEquals(1, deleted);
    assertEquals("delete from artworks a1_0 where a1_0.gallery_id is null", SqlLog.last());
  }

  @Test
  void mutationQueryOutsideTransactionFails() {
    RuntimeException e = assertThrows(RuntimeException.class, () -> {
      try (var em = emf.createEntityManager()) {
        em.createQuery("delete from Artwork a where a.gallery is null").executeUpdate();
      }
    });
    assertTrue(e.getClass().getName().contains("TransactionRequiredException"), e.getClass().getName());
  }

  @Test
  void insertValues() {
    int inserted = emf.callInTransaction(em -> em.unwrap(Session.class).createMutationQuery(
            "insert into Artist (name, country) values ('Ravi', 'India'), ('Sara', 'Italy')")
        .executeUpdate());
    assertEquals(2, inserted);
    assertEquals("insert into artists(name,country,id) values ('Ravi','India',?), ('Sara','Italy',?)",
        SqlLog.last());
    assertEquals(6L, (long) emf.callInTransaction(em ->
        em.createQuery("select count(ar) from Artist ar", Long.class).getSingleResult()));
  }

  @Test
  void insertSelect() {
    emf.runInTransaction(em -> {
      int copied = em.unwrap(Session.class).createMutationQuery("""
          insert into Highlight (title, artistName)
          select a.title, a.artist.name from Artwork a where a.estimatedValue > 2000""")
          .executeUpdate();
      assertEquals(2, copied);
      assertTrue(SqlLog.statements().stream().anyMatch(s -> s.startsWith("insert into HTE_highlights")));
      assertTrue(SqlLog.statements().stream().anyMatch(s -> s.startsWith(
          "insert into highlights(title,artistName,id) select")));
      List<String> highlights = em.createQuery("from Highlight h order by h.title", Highlight.class)
          .getResultList().stream().map(Highlight::toString).toList();
      assertEquals(List.of("Iron Leaf by Emma", "Stone Bird by Hugo"), highlights);
    });
  }

  // ---- statements used in the intro and the FAQs --------------------------

  @Test
  void introQueries() {
    emf.runInTransaction(em -> {
      List<Object[]> perGallery = em.createQuery(
          "select g.name, count(a) from Gallery g join g.artworks a group by g.name order by g.name", Object[].class)
          .getResultList();
      assertEquals(List.of(List.of("Modern Wing", 3L), List.of("Sculpture Hall", 2L)), rows(perGallery));
      int updated = em.createQuery("update Artwork a set a.estimatedValue = a.estimatedValue * 1.1 where a.medium = :m")
          .setParameter("m", Medium.PHOTO)
          .executeUpdate();
      assertEquals(1, updated);
    });
  }

  @Test
  void keywordsAreCaseInsensitiveNamesAreNot() {
    emf.runInTransaction(em -> {
      assertEquals(3, em.createQuery("SELECT a FROM Artwork a WHERE a.year > 2019", Artwork.class)
          .getResultList().size());
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
          em.createQuery("select a.created_year from Artwork a", Integer.class).getResultList());
      assertTrue(e.getMessage().contains("Could not resolve attribute 'created_year'"), e.getMessage());
    });
  }

  @Test
  void selectionAndMutationQueriesRejectTheOtherKind() {
    emf.runInTransaction(em -> {
      Session session = em.unwrap(Session.class);
      var select = assertThrows(org.hibernate.query.IllegalSelectQueryException.class, () ->
          session.createSelectionQuery("delete from Artwork a where a.gallery is null", Artwork.class));
      var mutation = assertThrows(org.hibernate.query.IllegalMutationQueryException.class, () ->
          session.createMutationQuery("from Artwork"));
      assertTrue(select.getMessage().startsWith("Expecting a selection query, but found"));
      assertTrue(mutation.getMessage().startsWith("Expecting a mutation query, but found 'from Artwork'"));
    });
  }

  @Test
  void conditionInWhereDropsArtistsWithoutMatch() {
    List<String> names = emf.callInTransaction(em -> em.createQuery("""
        select ar.name from Artist ar
        left join ar.artworks a
        where a.medium = :m
        order by ar.name""", String.class)
        .setParameter("m", Medium.SCULPTURE).getResultList());
    assertEquals(List.of("Emma", "Hugo"), names);
  }

  @Test
  void innerJoinFetchDropsArtistWithoutArtworks() {
    List<Artist> artists = emf.callInTransaction(em -> em.createQuery(
        "select ar from Artist ar join fetch ar.artworks order by ar.name", Artist.class).getResultList());
    assertEquals(List.of("Emma", "Hugo", "Lokesh"), artists.stream().map(Artist::getName).toList());
  }

  @Test
  void leftJoinProducesSevenRows() {
    Long joinedRows = emf.callInTransaction(em -> em.createQuery(
        "select count(*) from Artist ar left join ar.artworks a", Long.class).getSingleResult());
    assertEquals(7L, joinedRows);
  }

  @Test
  void averageValue() {
    Double avg = emf.callInTransaction(em -> em.createQuery(
        "select avg(a.estimatedValue) from Artwork a", Double.class).getSingleResult());
    assertEquals(1433.33, avg, 0.01);
  }

  @Test
  void sqlCommentsShowTheHql() {
    emf.close();
    emf = Database.createWithSqlComments();
    Database.seed(emf);
    SqlLog.clear();
    emf.runInTransaction(em -> em.createQuery("select a.title from Artwork a", String.class).getResultList());
    assertTrue(SqlLog.last().startsWith("/* select a.title from Artwork a */ select a1_0.title"), SqlLog.last());
  }

  @Test
  void hibernate74AcceptsMixedParametersAlthoughTheSpecForbidsThem() {
    List<Artwork> list = emf.callInTransaction(em ->
        em.createQuery("from Artwork a where a.medium = :m and a.year > ?1 order by a.title", Artwork.class)
            .setParameter("m", Medium.PAINTING)
            .setParameter(1, 2019)
            .getResultList());
    assertEquals(List.of("Old Bridge", "Red Fields"), titles(list));
  }
}
