package com.howtodoinjava.hibernate.sorting;

import static com.howtodoinjava.hibernate.sorting.TrailQueries.names;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.query.Order;
import org.hibernate.query.specification.SelectionSpecification;

public class SortingDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("0. Load five trails, three reviews, four waypoints and three tags");
      Long eagleId = Database.load(emf);

      emf.runInTransaction(em -> {
        step("1. JPQL order by, ascending is the default");
        print(names(TrailQueries.hql(em, "from Trail t order by t.lengthKm")));

        step("2. JPQL order by desc");
        print(names(TrailQueries.hql(em, "from Trail t order by t.lengthKm desc")));

        step("3. JPQL order by two columns");
        print(names(TrailQueries.hql(em, "from Trail t order by t.region, t.lengthKm desc")));

        step("4. JPQL nulls last / nulls first");
        print(names(TrailQueries.hql(em, "from Trail t order by t.region nulls last, t.name")));
        print(names(TrailQueries.hql(em, "from Trail t order by t.region desc nulls first, t.name")));

        step("5. Case-sensitive vs case-insensitive");
        print(names(TrailQueries.hql(em, "from Trail t order by t.name")));
        print(names(TrailQueries.hql(em, "from Trail t order by lower(t.name)")));

        step("6. Custom order with case");
        print(names(TrailQueries.hql(em, """
            from Trail t
            order by case t.difficulty when 'beginner' then 1 when 'intermediate' then 2 else 3 end, t.name""")));

        step("7. Order by an attribute of an associated entity");
        print(em.createQuery("from TrailReview r order by r.trail.name, r.rating desc", TrailReview.class)
            .getResultList());

        step("8. Criteria API: asc nulls last, desc");
        print(names(TrailQueries.criteriaByRegionThenLength(em)));

        step("9. Criteria API: lower(name)");
        print(names(TrailQueries.criteriaIgnoringCase(em)));

        step("10. Dynamic sort with an allow list (Criteria API)");
        print(names(TrailQueries.findAll(em, "length", true)));
        try {
          TrailQueries.findAll(em, "name; drop table Trail", false);
        } catch (IllegalArgumentException e) {
          System.out.println(e.getClass().getName() + ": " + e.getMessage());
        }

        step("11. Dynamic sort with SelectionSpecification and Order (Hibernate 7)");
        print(names(TrailQueries.findAllSpec(em, "difficulty", false)));
        print(names(TrailQueries.specIgnoringCase(em)));
        print(names(TrailQueries.specNullsLast(em)));

        step("12. Order with a wrong attribute name");
        try {
          SelectionSpecification.create(Trail.class, "from Trail")
              .sort(Order.asc(Trail.class, "nope"))
              .createQuery(em).getResultList();
        } catch (RuntimeException e) {
          System.out.println(e.getClass().getName() + ": " + e.getMessage());
        }

        step("13. WRONG: concatenating the request parameter");
        print(names(TrailQueries.unsafe(em, "name")));
        print(names(TrailQueries.unsafe(em,
            "id * (select count(*) from TrailReview r where r.author like 'L%')")));
        try {
          TrailQueries.unsafe(em, "name; drop table Trail");
        } catch (RuntimeException e) {
          System.out.println(e.getClass().getName() + ": " + e.getMessage());
        }
      });

      emf.runInTransaction(em -> {
        Trail eagle = em.find(Trail.class, eagleId);

        step("14. @OrderBy: the database sorts the reviews");
        print(eagle.getReviews());

        step("15. @SortComparator: Hibernate sorts the reviews in memory");
        print(eagle.getSortedReviews());
        System.out.println(eagle.getSortedReviews().getClass().getName());

        step("16. @SQLOrder: a SQL fragment in the ORDER BY");
        print(eagle.getReviewsByAuthor());

        step("17. @OrderColumn: position stored in the table");
        print(eagle.getWaypoints());

        step("18. @SortNatural: String.compareTo() in memory");
        print(eagle.getTags());
      });

      step("19. Remove a waypoint: positions are rewritten");
      emf.runInTransaction(em -> em.find(Trail.class, eagleId).getWaypoints().remove("Bridge"));
      emf.runInTransaction(em -> print(em.find(Trail.class, eagleId).getWaypoints()));

      step("20. join fetch keeps the @OrderBy order");
      emf.runInTransaction(em -> print(em.createQuery(
              "from Trail t join fetch t.reviews where t.id = :id", Trail.class)
          .setParameter("id", eagleId)
          .getSingleResult().getReviews()));

      step("21. A new review is not sorted until the collection is reloaded");
      emf.runInTransaction(em -> {
        Trail eagle = em.find(Trail.class, eagleId);
        eagle.getReviews().size();                 // load the sorted list
        TrailReview anna = new TrailReview("Anna", 4);
        eagle.addReview(anna);
        em.persist(anna);
        print(eagle.getReviews());
      });
      emf.runInTransaction(em -> print(em.find(Trail.class, eagleId).getReviews()));
    }

    step("22. hibernate.order_by.default_null_ordering = last");
    try (EntityManagerFactory emf = Database.create(true, "last")) {
      Database.load(emf);
      emf.runInTransaction(em ->
          print(names(TrailQueries.hql(em, "from Trail t order by t.region, t.name"))));
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }

  private static void print(Object result) {
    System.out.println("-> " + result);
  }
}
