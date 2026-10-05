package com.howtodoinjava.hibernate.aggregate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;

public class AggregateFunctionsDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {
      Database.loadResults(emf);

      emf.runInTransaction(em -> {
        step("1. One aggregate, one value");
        single(em, "select count(r) from Runner r");
        single(em, "select count(*) from Runner r");
        single(em, "select count(r.finishMinutes) from Runner r");
        single(em, "select count(distinct r.city) from Runner r");
        single(em, "select min(r.finishMinutes) from Runner r");
        single(em, "select max(r.finishMinutes) from Runner r");
        single(em, "select sum(r.finishMinutes) from Runner r");
        single(em, "select avg(r.finishMinutes) from Runner r");
        single(em, "select sum(r.entryFee) from Runner r");
        single(em, "select min(r.entryFee) from Runner r");
        single(em, "select max(r.name) from Runner r");
        single(em, "select sum(r.finishMinutes / 60.0) from Runner r");
        single(em, "select avg(r.entryFee) from Runner r");

        step("2. Several aggregates in one query");
        Object[] stats = em.createQuery(
                "select count(r), min(r.finishMinutes), max(r.finishMinutes), avg(r.finishMinutes) "
                    + "from Runner r", Object[].class)
            .getSingleResult();
        System.out.println("-> " + Arrays.toString(stats));

        Tuple t = em.createQuery(
                "select count(r) as runners, avg(r.finishMinutes) as avgMinutes from Runner r",
                Tuple.class)
            .getSingleResult();
        System.out.println("-> runners=" + t.get("runners", Long.class)
            + " avgMinutes=" + t.get("avgMinutes", Double.class));

        step("3. group by");
        rows(em, "select r.ageGroup, count(r), avg(r.finishMinutes) from Runner r "
            + "group by r.ageGroup order by r.ageGroup");
        rows(em, "select r.city, count(r) from Runner r group by r.city order by count(r) desc");

        step("4. having");
        rows(em, "select r.ageGroup, avg(r.finishMinutes) from Runner r "
            + "group by r.ageGroup having avg(r.finishMinutes) < 210 order by r.ageGroup");
        rows(em, "select r.city, count(r) from Runner r where r.finishMinutes is not null "
            + "group by r.city having count(r) >= 2 order by r.city");

        step("5. No matching rows");
        single(em, "select count(r) from Runner r where r.city = 'Paris'");
        single(em, "select sum(r.finishMinutes) from Runner r where r.city = 'Paris'");
        single(em, "select avg(r.finishMinutes) from Runner r where r.city = 'Paris'");
        single(em, "select max(r.finishMinutes) from Runner r where r.city = 'Paris'");
        single(em, "select coalesce(sum(r.finishMinutes), 0) from Runner r where r.city = 'Paris'");
        rows(em, "select r.city, count(r) from Runner r where r.city = 'Paris' group by r.city");

        step("6. Aggregates with joins");
        rows(em, "select c.name, count(r) from Club c join c.members r group by c.name order by c.name");
        rows(em, "select c.name, count(r) from Club c left join c.members r group by c.name order by c.name");
        rows(em, "select c.name, count(*) from Club c left join c.members r group by c.name order by c.name");
        rows(em, "select r.club.name, avg(r.finishMinutes) from Runner r group by r.club.name order by 1");

        step("7. Results into a record");
        List<AgeGroupStats> a = em.createQuery(
                "select new com.howtodoinjava.hibernate.aggregate.AgeGroupStats("
                    + "r.ageGroup, count(r), avg(r.finishMinutes)) "
                    + "from Runner r group by r.ageGroup order by r.ageGroup", AgeGroupStats.class)
            .getResultList();
        System.out.println("-> " + a);
        List<AgeGroupStats> b = em.createQuery(
                "select r.ageGroup, count(r), avg(r.finishMinutes) "
                    + "from Runner r group by r.ageGroup order by r.ageGroup", AgeGroupStats.class)
            .getResultList();
        System.out.println("-> " + b);

        step("8. Criteria API");
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Runner> c1 = countQuery.from(Runner.class);
        countQuery.select(cb.count(c1));
        System.out.println("-> " + em.createQuery(countQuery).getSingleResult());

        CriteriaQuery<Long> distinctQuery = cb.createQuery(Long.class);
        Root<Runner> c2 = distinctQuery.from(Runner.class);
        distinctQuery.select(cb.countDistinct(c2.get("city")));
        System.out.println("-> " + em.createQuery(distinctQuery).getSingleResult());

        CriteriaQuery<Double> avgQuery = cb.createQuery(Double.class);
        Root<Runner> c3 = avgQuery.from(Runner.class);
        avgQuery.select(cb.avg(c3.get("finishMinutes")));
        System.out.println("-> " + em.createQuery(avgQuery).getSingleResult());

        CriteriaQuery<Long> sumQuery = cb.createQuery(Long.class);
        Root<Runner> c4 = sumQuery.from(Runner.class);
        sumQuery.select(cb.sumAsLong(c4.get("finishMinutes")));
        System.out.println("-> " + em.createQuery(sumQuery).getSingleResult());

        CriteriaQuery<Integer> maxQuery = cb.createQuery(Integer.class);
        Root<Runner> c5 = maxQuery.from(Runner.class);
        maxQuery.select(cb.max(c5.get("finishMinutes")));
        System.out.println("-> " + em.createQuery(maxQuery).getSingleResult());

        CriteriaQuery<String> greatestQuery = cb.createQuery(String.class);
        Root<Runner> c6 = greatestQuery.from(Runner.class);
        greatestQuery.select(cb.greatest(c6.<String>get("name")));
        System.out.println("-> " + em.createQuery(greatestQuery).getSingleResult());

        CriteriaQuery<AgeGroupStats> statsQuery = cb.createQuery(AgeGroupStats.class);
        Root<Runner> r = statsQuery.from(Runner.class);
        statsQuery.select(cb.construct(AgeGroupStats.class,
                r.get("ageGroup"), cb.count(r), cb.avg(r.get("finishMinutes"))))
            .groupBy(r.get("ageGroup"))
            .having(cb.lt(cb.avg(r.get("finishMinutes")), 210.0))
            .orderBy(cb.asc(r.get("ageGroup")));
        System.out.println("-> " + em.createQuery(statsQuery).getResultList());

        CriteriaQuery<Object> rawSum = cb.createQuery(Object.class);
        Root<Runner> c7 = rawSum.from(Runner.class);
        rawSum.select(cb.sum(c7.<Integer>get("finishMinutes")));
        Object raw = em.createQuery(rawSum).getSingleResult();
        System.out.println("-> cb.sum: " + raw + " (" + raw.getClass().getSimpleName() + ")");

        step("9. HQL functions beyond JPQL: filter, listagg, percentiles, window functions");
        rows(em, "select r.ageGroup, count(*) filter (where r.finishMinutes < 210) from Runner r "
            + "group by r.ageGroup order by r.ageGroup");
        rows(em, "select r.city, listagg(r.name, ', ') within group (order by r.name) from Runner r "
            + "group by r.city order by r.city");
        rows(em, "select r.ageGroup, percentile_cont(0.5) within group (order by r.finishMinutes) "
            + "from Runner r group by r.ageGroup order by r.ageGroup");
        rows(em, "select r.ageGroup, percentile_cont(0.5) within group (order by cast(r.finishMinutes as Double)) "
            + "from Runner r group by r.ageGroup order by r.ageGroup");
        single(em, "select percentile_disc(0.5) within group (order by r.finishMinutes) from Runner r");
        single(em, "select mode() within group (order by r.city) from Runner r");
        single(em, "select every(r.finishMinutes < 250) from Runner r");
        single(em, "select any(r.finishMinutes < 190) from Runner r");
        rows(em, "select r.name, r.ageGroup, r.finishMinutes, "
            + "rank() over (partition by r.ageGroup order by r.finishMinutes) "
            + "from Runner r where r.finishMinutes is not null order by r.ageGroup, r.finishMinutes");
        rows(em, "select r.name, r.finishMinutes, avg(r.finishMinutes) over () "
            + "from Runner r where r.finishMinutes is not null order by r.finishMinutes");
      });

      step("10. Aggregate in the where clause");
      emf.runInTransaction(em -> {
        System.out.println("-> " + em.createQuery("select r.name from Runner r "
                + "where r.finishMinutes < (select avg(r2.finishMinutes) from Runner r2) "
                + "order by r.finishMinutes", String.class)
            .getResultList());
      });
      try {
        emf.runInTransaction(em -> em.createQuery(
                "select r.name from Runner r where r.finishMinutes < avg(r.finishMinutes)", String.class)
            .getResultList());
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("11. Wrong result class and missing group by");
      try {
        emf.runInTransaction(em ->
            em.createQuery("select count(r) from Runner r", Integer.class).getSingleResult());
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
      try {
        emf.runInTransaction(em ->
            em.createQuery("select sum(r.finishMinutes) from Runner r", Integer.class)
                .getSingleResult());
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
      try {
        emf.runInTransaction(em ->
            em.createQuery("select r.ageGroup, count(r) from Runner r", Object[].class)
                .getResultList());
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }
  }

  private static void single(EntityManager em, String jpql) {
    Object v = em.createQuery(jpql, Object.class).getSingleResult();
    System.out.println(jpql + "\n-> " + v + (v == null ? "" : " (" + v.getClass().getSimpleName() + ")"));
  }

  private static void rows(EntityManager em, String jpql) {
    List<Object[]> list = em.createQuery(jpql, Object[].class).getResultList();
    System.out.println(jpql);
    list.forEach(row -> System.out.println("-> " + Arrays.toString(row)));
    if (list.isEmpty()) {
      System.out.println("-> (no rows)");
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
