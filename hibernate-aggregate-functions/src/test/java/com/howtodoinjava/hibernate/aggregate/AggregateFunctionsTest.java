package com.howtodoinjava.hibernate.aggregate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.query.QueryTypeMismatchException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AggregateFunctionsTest {

  private static EntityManagerFactory emf;

  @BeforeAll
  static void setUp() {
    emf = Database.create(false);
    Database.loadResults(emf);
  }

  @AfterAll
  static void tearDown() {
    emf.close();
  }

  private static Object single(String jpql) {
    return emf.callInTransaction(em -> em.createQuery(jpql, Object.class).getSingleResult());
  }

  private static List<Object[]> rows(String jpql) {
    return emf.callInTransaction(em -> em.createQuery(jpql, Object[].class).getResultList());
  }

  private static void assertRows(List<Object[]> actual, Object[]... expected) {
    assertEquals(expected.length, actual.size());
    for (int i = 0; i < expected.length; i++) {
      assertArrayEquals(expected[i], actual.get(i));
    }
  }

  // 1. count, min, max, sum, avg and their Java types

  @Test
  void countVariants() {
    assertEquals(6L, single("select count(r) from Runner r"));
    assertEquals(6L, single("select count(*) from Runner r"));
    assertEquals(5L, single("select count(r.finishMinutes) from Runner r"));
    assertEquals(3L, single("select count(distinct r.city) from Runner r"));
    assertEquals(5L, single("select count(r.club) from Runner r"));
    assertEquals(6L, single("SELECT COUNT(r) FROM Runner r"));
  }

  @Test
  void minMaxSumAvg() {
    assertEquals(182, single("select min(r.finishMinutes) from Runner r"));
    assertEquals(240, single("select max(r.finishMinutes) from Runner r"));
    assertEquals(1040L, single("select sum(r.finishMinutes) from Runner r"));
    assertEquals(208.0, single("select avg(r.finishMinutes) from Runner r"));
  }

  @Test
  void typedQueries() {
    emf.runInTransaction(em -> {
      Long runners = em.createQuery("select count(r) from Runner r", Long.class)
          .getSingleResult();
      Integer fastest = em.createQuery("select min(r.finishMinutes) from Runner r", Integer.class)
          .getSingleResult();
      Long total = em.createQuery("select sum(r.finishMinutes) from Runner r", Long.class)
          .getSingleResult();
      Double average = em.createQuery("select avg(r.finishMinutes) from Runner r", Double.class)
          .getSingleResult();
      BigDecimal fees = em.createQuery("select sum(r.entryFee) from Runner r", BigDecimal.class)
          .getSingleResult();
      assertEquals(6L, runners);
      assertEquals(182, fastest);
      assertEquals(1040L, total);
      assertEquals(208.0, average);
      assertEquals(new BigDecimal("270.00"), fees);
    });
  }

  @Test
  void resultTypes() {
    assertInstanceOf(Long.class, single("select count(r) from Runner r"));
    assertInstanceOf(Integer.class, single("select min(r.finishMinutes) from Runner r"));
    assertInstanceOf(Integer.class, single("select max(r.finishMinutes) from Runner r"));
    assertInstanceOf(Long.class, single("select sum(r.finishMinutes) from Runner r"));
    assertInstanceOf(Double.class, single("select avg(r.finishMinutes) from Runner r"));
    assertEquals(new BigDecimal("270.00"), single("select sum(r.entryFee) from Runner r"));
    assertEquals(new BigDecimal("40.00"), single("select min(r.entryFee) from Runner r"));
    assertEquals(45.0, single("select avg(r.entryFee) from Runner r"));
    assertEquals("Priya", single("select max(r.name) from Runner r"));
    Object hours = single("select sum(r.finishMinutes / 60.0) from Runner r");
    assertInstanceOf(Double.class, hours);
    assertEquals(17.33333, (Double) hours, 0.000001);
  }

  @Test
  void distinctValues() {
    Object[] row = emf.callInTransaction(em -> em.createQuery(
        "select avg(distinct r.entryFee), sum(distinct r.entryFee) from Runner r",
        Object[].class).getSingleResult());
    assertArrayEquals(new Object[] {45.0, new BigDecimal("90.00")}, row);
  }

  @Test
  void severalAggregatesInOneQuery() {
    Object[] stats = emf.callInTransaction(em -> em.createQuery(
            "select count(r), min(r.finishMinutes), max(r.finishMinutes), avg(r.finishMinutes) "
                + "from Runner r", Object[].class)
        .getSingleResult());
    assertArrayEquals(new Object[] {6L, 182, 240, 208.0}, stats);

    Tuple t = emf.callInTransaction(em -> em.createQuery(
            "select count(r) as runners, avg(r.finishMinutes) as avgMinutes from Runner r",
            Tuple.class)
        .getSingleResult());
    assertEquals(6L, t.get("runners", Long.class));
    assertEquals(208.0, t.get("avgMinutes", Double.class));
  }

  @Test
  void wrongResultClassFails() {
    QueryTypeMismatchException count = assertThrows(QueryTypeMismatchException.class,
        () -> emf.runInTransaction(em ->
            em.createQuery("select count(r) from Runner r", Integer.class).getSingleResult()));
    assertEquals("Incorrect query result type: query produces 'java.lang.Long' "
        + "but type 'java.lang.Integer' was given", count.getMessage());

    assertThrows(QueryTypeMismatchException.class, () -> emf.runInTransaction(em ->
        em.createQuery("select sum(r.finishMinutes) from Runner r", Integer.class)
            .getSingleResult()));
  }

  // 2. group by and having

  @Test
  void groupByAgeGroup() {
    assertRows(rows("select r.ageGroup, count(r), avg(r.finishMinutes) from Runner r "
            + "group by r.ageGroup order by r.ageGroup"),
        new Object[] {"20-29", 2L, 193.5},
        new Object[] {"30-39", 2L, 206.5},
        new Object[] {"40-49", 2L, 240.0});
  }

  @Test
  void groupByCityOrderedByCount() {
    assertRows(rows("select r.city, count(r) from Runner r group by r.city order by count(r) desc"),
        new Object[] {"London", 3L},
        new Object[] {"Delhi", 2L},
        new Object[] {"Madrid", 1L});
  }

  @Test
  void havingFiltersGroups() {
    assertRows(rows("select r.ageGroup, avg(r.finishMinutes) from Runner r "
            + "group by r.ageGroup having avg(r.finishMinutes) < 210 order by r.ageGroup"),
        new Object[] {"20-29", 193.5},
        new Object[] {"30-39", 206.5});
    assertRows(rows("select r.city, count(r) from Runner r where r.finishMinutes is not null "
            + "group by r.city having count(r) >= 2 order by r.city"),
        new Object[] {"Delhi", 2L},
        new Object[] {"London", 2L});
  }

  @Test
  void missingGroupByFails() {
    PersistenceException e = assertThrows(PersistenceException.class, () -> rows(
        "select r.ageGroup, count(r) from Runner r"));
    assertTrue(e.getMessage().contains("Column \"R1_0.AGEGROUP\" must be in the GROUP BY list"));
  }

  @Test
  void aggregateInWhereNeedsSubquery() {
    List<String> faster = emf.callInTransaction(em -> em.createQuery("select r.name from Runner r "
            + "where r.finishMinutes < (select avg(r2.finishMinutes) from Runner r2) "
            + "order by r.finishMinutes", String.class)
        .getResultList());
    assertEquals(List.of("Priya", "Alex", "Emma"), faster);

    PersistenceException e = assertThrows(PersistenceException.class, () -> emf.runInTransaction(
        em -> em.createQuery("select r.name from Runner r where r.finishMinutes < avg(r.finishMinutes)",
            String.class).getResultList()));
    assertTrue(e.getMessage().contains("Invalid use of aggregate function"));
  }

  // 3. nulls and empty results

  @Test
  void noMatchingRows() {
    assertEquals(0L, single("select count(r) from Runner r where r.city = 'Paris'"));
    assertNull(single("select sum(r.finishMinutes) from Runner r where r.city = 'Paris'"));
    assertNull(single("select avg(r.finishMinutes) from Runner r where r.city = 'Paris'"));
    assertNull(single("select max(r.finishMinutes) from Runner r where r.city = 'Paris'"));
    assertEquals(0L, single("select coalesce(sum(r.finishMinutes), 0) from Runner r where r.city = 'Paris'"));
    assertTrue(rows("select r.city, count(r) from Runner r where r.city = 'Paris' group by r.city")
        .isEmpty());
  }

  @Test
  void unboxingNullSumThrows() {
    assertThrows(NullPointerException.class, () -> emf.runInTransaction(em -> {
      long total = em.createQuery(
              "select sum(r.finishMinutes) from Runner r where r.city = 'Paris'", Long.class)
          .getSingleResult();
    }));
  }

  @Test
  void entityAndFieldNamesAreCaseSensitive() {
    IllegalArgumentException entity = assertThrows(IllegalArgumentException.class,
        () -> single("select count(r) from runner r"));
    assertTrue(entity.getMessage().contains("Could not resolve root entity 'runner'"));
    IllegalArgumentException field = assertThrows(IllegalArgumentException.class,
        () -> single("select max(r.FinishMinutes) from Runner r"));
    assertTrue(field.getMessage().contains("Could not resolve attribute 'FinishMinutes'"));
  }

  @Test
  void nullsAreSkippedUnlessReplaced() {
    assertRows(rows("select r.ageGroup, avg(coalesce(r.finishMinutes, 0)) from Runner r "
            + "group by r.ageGroup order by r.ageGroup"),
        new Object[] {"20-29", 193.5},
        new Object[] {"30-39", 206.5},
        new Object[] {"40-49", 120.0});
  }

  // 4. joins

  @Test
  void aggregatesWithJoins() {
    assertRows(rows("select c.name, count(r) from Club c join c.members r group by c.name order by c.name"),
        new Object[] {"Delhi Striders", 2L},
        new Object[] {"Thames Runners", 3L});
    assertRows(rows("select c.name, count(r) from Club c left join c.members r group by c.name order by c.name"),
        new Object[] {"Delhi Striders", 2L},
        new Object[] {"Night Owls", 0L},
        new Object[] {"Thames Runners", 3L});
    assertRows(rows("select c.name, count(*) from Club c left join c.members r group by c.name order by c.name"),
        new Object[] {"Delhi Striders", 2L},
        new Object[] {"Night Owls", 1L},
        new Object[] {"Thames Runners", 3L});
    assertRows(rows("select r.club.name, avg(r.finishMinutes) from Runner r group by r.club.name order by 1"),
        new Object[] {"Delhi Striders", 198.5},
        new Object[] {"Thames Runners", 201.5});
  }

  // 5. records

  private static final List<AgeGroupStats> ALL_GROUPS = List.of(
      new AgeGroupStats("20-29", 2L, 193.5),
      new AgeGroupStats("30-39", 2L, 206.5),
      new AgeGroupStats("40-49", 2L, 240.0));

  @Test
  void selectNewRecord() {
    List<AgeGroupStats> stats = emf.callInTransaction(em -> em.createQuery(
            "select new com.howtodoinjava.hibernate.aggregate.AgeGroupStats("
                + "r.ageGroup, count(r), avg(r.finishMinutes)) "
                + "from Runner r group by r.ageGroup order by r.ageGroup", AgeGroupStats.class)
        .getResultList());
    assertEquals(ALL_GROUPS, stats);
  }

  @Test
  void recordWithoutSelectNew() {
    List<AgeGroupStats> stats = emf.callInTransaction(em -> em.createQuery(
            "select r.ageGroup, count(r), avg(r.finishMinutes) "
                + "from Runner r group by r.ageGroup order by r.ageGroup", AgeGroupStats.class)
        .getResultList());
    assertEquals(ALL_GROUPS, stats);
  }

  // 6. Criteria API

  @Test
  void criteriaSingleValues() {
    emf.runInTransaction(em -> {
      CriteriaBuilder cb = em.getCriteriaBuilder();

      CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
      Root<Runner> c1 = countQuery.from(Runner.class);
      countQuery.select(cb.count(c1));
      assertEquals(6L, em.createQuery(countQuery).getSingleResult());

      CriteriaQuery<Long> distinctQuery = cb.createQuery(Long.class);
      Root<Runner> c2 = distinctQuery.from(Runner.class);
      distinctQuery.select(cb.countDistinct(c2.get("city")));
      assertEquals(3L, em.createQuery(distinctQuery).getSingleResult());

      CriteriaQuery<Double> avgQuery = cb.createQuery(Double.class);
      Root<Runner> c3 = avgQuery.from(Runner.class);
      avgQuery.select(cb.avg(c3.get("finishMinutes")));
      assertEquals(208.0, em.createQuery(avgQuery).getSingleResult());

      CriteriaQuery<Long> sumQuery = cb.createQuery(Long.class);
      Root<Runner> c4 = sumQuery.from(Runner.class);
      sumQuery.select(cb.sumAsLong(c4.get("finishMinutes")));
      assertEquals(1040L, em.createQuery(sumQuery).getSingleResult());

      CriteriaQuery<Integer> maxQuery = cb.createQuery(Integer.class);
      Root<Runner> c5 = maxQuery.from(Runner.class);
      maxQuery.select(cb.max(c5.get("finishMinutes")));
      assertEquals(240, em.createQuery(maxQuery).getSingleResult());

      CriteriaQuery<String> greatestQuery = cb.createQuery(String.class);
      Root<Runner> c6 = greatestQuery.from(Runner.class);
      greatestQuery.select(cb.greatest(c6.<String>get("name")));
      assertEquals("Priya", em.createQuery(greatestQuery).getSingleResult());

      CriteriaQuery<Integer> minQuery = cb.createQuery(Integer.class);
      Root<Runner> c7 = minQuery.from(Runner.class);
      minQuery.select(cb.min(c7.get("finishMinutes")));
      assertEquals(182, em.createQuery(minQuery).getSingleResult());

      CriteriaQuery<String> leastQuery = cb.createQuery(String.class);
      Root<Runner> c8 = leastQuery.from(Runner.class);
      leastQuery.select(cb.least(c8.<String>get("name")));
      assertEquals("Alex", em.createQuery(leastQuery).getSingleResult());
    });
  }

  @Test
  void criteriaSumKeepsArgumentType() {
    Object sum = emf.callInTransaction(em -> {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Integer> q = cb.createQuery(Integer.class);
      Root<Runner> r = q.from(Runner.class);
      q.select(cb.sum(r.<Integer>get("finishMinutes")));
      return em.createQuery(q).getSingleResult();
    });
    assertEquals(1040, sum);
    assertInstanceOf(Integer.class, sum);
  }

  @Test
  void criteriaGroupByHavingRecord() {
    List<AgeGroupStats> stats = emf.callInTransaction(em -> {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<AgeGroupStats> q = cb.createQuery(AgeGroupStats.class);
      Root<Runner> r = q.from(Runner.class);
      q.select(cb.construct(AgeGroupStats.class,
              r.get("ageGroup"), cb.count(r), cb.avg(r.get("finishMinutes"))))
          .groupBy(r.get("ageGroup"))
          .having(cb.lt(cb.avg(r.get("finishMinutes")), 210.0))
          .orderBy(cb.asc(r.get("ageGroup")));
      return em.createQuery(q).getResultList();
    });
    assertEquals(ALL_GROUPS.subList(0, 2), stats);
  }

  // 7. HQL functions beyond JPQL

  @Test
  void filterClause() {
    assertRows(rows("select r.ageGroup, count(*) filter (where r.finishMinutes < 210) from Runner r "
            + "group by r.ageGroup order by r.ageGroup"),
        new Object[] {"20-29", 2L},
        new Object[] {"30-39", 1L},
        new Object[] {"40-49", 0L});
  }

  @Test
  void listagg() {
    assertRows(rows("select r.city, listagg(r.name, ', ') within group (order by r.name) from Runner r "
            + "group by r.city order by r.city"),
        new Object[] {"Delhi", "Lokesh, Priya"},
        new Object[] {"London", "Alex, Emma, John"},
        new Object[] {"Madrid", "Maria"});
  }

  @Test
  void percentiles() {
    assertRows(rows("select r.ageGroup, percentile_cont(0.5) within group (order by r.finishMinutes) "
            + "from Runner r group by r.ageGroup order by r.ageGroup"),
        new Object[] {"20-29", 194},
        new Object[] {"30-39", 207},
        new Object[] {"40-49", 240});
    assertRows(rows("select r.ageGroup, percentile_cont(0.5) within group "
            + "(order by cast(r.finishMinutes as Double)) "
            + "from Runner r group by r.ageGroup order by r.ageGroup"),
        new Object[] {"20-29", 193.5},
        new Object[] {"30-39", 206.5},
        new Object[] {"40-49", 240.0});
    assertEquals(205, single("select percentile_disc(0.5) within group (order by r.finishMinutes) from Runner r"));
    assertEquals("London", single("select mode() within group (order by r.city) from Runner r"));
  }

  @Test
  void everyAndAny() {
    assertEquals(true, single("select every(r.finishMinutes < 250) from Runner r"));
    assertEquals(true, single("select any(r.finishMinutes < 190) from Runner r"));
  }

  @Test
  void windowFunctions() {
    assertRows(rows("select r.name, r.ageGroup, r.finishMinutes, "
            + "rank() over (partition by r.ageGroup order by r.finishMinutes) "
            + "from Runner r where r.finishMinutes is not null order by r.ageGroup, r.finishMinutes"),
        new Object[] {"Priya", "20-29", 182, 1L},
        new Object[] {"Emma", "20-29", 205, 2L},
        new Object[] {"Alex", "30-39", 198, 1L},
        new Object[] {"Lokesh", "30-39", 215, 2L},
        new Object[] {"Maria", "40-49", 240, 1L});
    assertRows(rows("select r.name, r.finishMinutes, avg(r.finishMinutes) over () "
            + "from Runner r where r.finishMinutes is not null order by r.finishMinutes"),
        new Object[] {"Priya", 182, 208.0},
        new Object[] {"Alex", 198, 208.0},
        new Object[] {"Emma", 205, 208.0},
        new Object[] {"Lokesh", 215, 208.0},
        new Object[] {"Maria", 240, 208.0});
  }
}
