package com.howtodoinjava.hibernate.hql;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.hibernate.Session;

public class HqlDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {
      Database.seed(emf);

      step("1. Select entities with where and order by");
      emf.runInTransaction(em -> {
        List<Artwork> recent = em.createQuery(
            "select a from Artwork a where a.year > 2019 order by a.year", Artwork.class)
            .getResultList();
        print(recent);
      });

      step("2. HQL without select (Hibernate only)");
      emf.runInTransaction(em -> {
        List<Artwork> recent = em.createQuery("from Artwork where year > 2019 order by year", Artwork.class)
            .getResultList();
        print(recent);
      });

      step("3. createSelectionQuery with a named parameter");
      emf.runInTransaction(em -> {
        Session session = em.unwrap(Session.class);
        List<Artwork> sculptures = session.createSelectionQuery(
            "from Artwork a where a.medium = :medium order by a.title", Artwork.class)
            .setParameter("medium", Medium.SCULPTURE)
            .getResultList();
        print(sculptures);
      });

      step("4. Positional parameters");
      emf.runInTransaction(em -> {
        List<Artwork> range = em.createQuery("""
            select a from Artwork a
            where a.estimatedValue between ?1 and ?2
            order by a.estimatedValue desc""", Artwork.class)
            .setParameter(1, new BigDecimal("800"))
            .setParameter(2, new BigDecimal("2500"))
            .getResultList();
        print(range);
      });

      step("5. getSingleResult and getSingleResultOrNull");
      emf.runInTransaction(em -> {
        Artwork bird = em.createQuery("from Artwork a where a.title = :title", Artwork.class)
            .setParameter("title", "Stone Bird").getSingleResult();
        Artwork none = em.createQuery("from Artwork a where a.title = :title", Artwork.class)
            .setParameter("title", "Sunflowers").getSingleResultOrNull();
        print(bird + " / " + none);
      });

      step("6. Projections: one attribute, Object[], Tuple");
      emf.runInTransaction(em -> {
        List<String> titles = em.createQuery(
            "select a.title from Artwork a order by a.title", String.class).getResultList();
        print(titles);
        List<Object[]> rows = em.createQuery(
            "select a.title, a.year from Artwork a where a.medium = :m order by a.year", Object[].class)
            .setParameter("m", Medium.SCULPTURE).getResultList();
        rows.forEach(r -> print(Arrays.toString(r)));
        List<Tuple> tuples = em.createQuery(
            "select a.title as title, a.estimatedValue as value from Artwork a where a.year = 2015", Tuple.class)
            .getResultList();
        print(tuples.get(0).get("title") + " " + tuples.get(0).get("value"));
      });

      step("7. Record with select new");
      emf.runInTransaction(em -> {
        List<ArtworkSummary> list = em.createQuery("""
            select new com.howtodoinjava.hibernate.hql.ArtworkSummary(a.title, a.artist.name)
            from Artwork a where a.medium = :m order by a.title""", ArtworkSummary.class)
            .setParameter("m", Medium.PAINTING).getResultList();
        print(list);
      });

      step("8. Record without select new (Hibernate 6+)");
      emf.runInTransaction(em -> {
        List<ArtworkSummary> list = em.createQuery("""
            select a.title, a.artist.name
            from Artwork a where a.medium = :m order by a.title""", ArtworkSummary.class)
            .setParameter("m", Medium.PAINTING).getResultList();
        print(list);
      });

      step("9. Implicit join through a path");
      emf.runInTransaction(em -> {
        List<String> french = em.createQuery(
            "select a.title from Artwork a where a.artist.country = :country order by a.title", String.class)
            .setParameter("country", "France")
            .getResultList();
        print(french);
      });

      step("10. Explicit inner join");
      emf.runInTransaction(em -> {
        List<Object[]> rows = em.createQuery("""
            select g.name, a.title from Gallery g join g.artworks a
            order by g.name, a.title""", Object[].class).getResultList();
        rows.forEach(r -> print(Arrays.toString(r)));
      });

      step("11. Left join");
      emf.runInTransaction(em -> {
        List<Object[]> rows = em.createQuery("""
            select g.name, a.title from Gallery g left join g.artworks a
            order by g.name, a.title""", Object[].class).getResultList();
        rows.forEach(r -> print(Arrays.toString(r)));
      });

      step("12. Left join with an on condition");
      emf.runInTransaction(em -> {
        List<Object[]> rows = em.createQuery("""
            select ar.name, a.title from Artist ar
            left join ar.artworks a on a.medium = :m
            order by ar.name""", Object[].class)
            .setParameter("m", Medium.SCULPTURE).getResultList();
        rows.forEach(r -> print(Arrays.toString(r)));
      });

      step("13. Lazy collection without join fetch (N+1)");
      emf.runInTransaction(em -> {
        List<Artist> artists = em.createQuery("from Artist ar order by ar.name", Artist.class).getResultList();
        artists.forEach(ar -> print(ar.getName() + " " + ar.getArtworks()));
      });

      step("14. join fetch");
      emf.runInTransaction(em -> {
        List<Artist> artists = em.createQuery(
            "select ar from Artist ar left join fetch ar.artworks order by ar.name", Artist.class)
            .getResultList();
        print(artists.size() + " artists");
        artists.forEach(ar -> print(ar.getName() + " " + ar.getArtworks()));
      });

      step("15. group by and having");
      emf.runInTransaction(em -> {
        List<Object[]> rows = em.createQuery("""
            select g.name, count(a), sum(a.estimatedValue)
            from Gallery g join g.artworks a
            group by g.name
            having count(a) > 1
            order by g.name""", Object[].class).getResultList();
        rows.forEach(r -> print(Arrays.toString(r)));
      });

      step("16. Subquery");
      emf.runInTransaction(em -> {
        List<String> aboveAverage = em.createQuery("""
            select a.title from Artwork a
            where a.estimatedValue > (select avg(a2.estimatedValue) from Artwork a2)
            order by a.title""", String.class).getResultList();
        print(aboveAverage);
      });

      step("17. exists and not exists");
      emf.runInTransaction(em -> {
        List<String> sculptors = em.createQuery("""
            select ar.name from Artist ar
            where exists (select 1 from Artwork a where a.artist = ar and a.medium = :m)
            order by ar.name""", String.class)
            .setParameter("m", Medium.SCULPTURE).getResultList();
        print(sculptors);
        List<String> noWork = em.createQuery("""
            select ar.name from Artist ar
            where not exists (select 1 from Artwork a where a.artist = ar)""", String.class)
            .getResultList();
        print(noWork);
      });

      step("18. case and coalesce");
      emf.runInTransaction(em -> {
        List<Object[]> rows = em.createQuery("""
            select a.title,
                   case when a.estimatedValue >= 2000 then 'high' else 'normal' end,
                   coalesce(g.name, 'Storage')
            from Artwork a left join a.gallery g
            order by a.title""", Object[].class).getResultList();
        rows.forEach(r -> print(Arrays.toString(r)));
      });

      step("19. Implicit join drops nulls");
      emf.runInTransaction(em -> {
        List<String> titles = em.createQuery(
            "select a.title from Artwork a order by a.gallery.name, a.title", String.class).getResultList();
        print(titles);
      });

      step("20. Functions");
      emf.runInTransaction(em -> {
        List<Object[]> rows = em.createQuery("""
            select upper(ar.name), ar.name || ' (' || ar.country || ')', length(ar.name)
            from Artist ar where ar.country = 'France' order by ar.name""", Object[].class).getResultList();
        rows.forEach(r -> print(Arrays.toString(r)));
      });

      step("21. Pagination with setFirstResult and setMaxResults");
      emf.runInTransaction(em -> {
        List<String> page = em.createQuery("select a.title from Artwork a order by a.title", String.class)
            .setFirstResult(2).setMaxResults(2).getResultList();
        print(page);
      });

      step("22. limit and offset in HQL");
      emf.runInTransaction(em -> {
        List<String> page = em.createQuery(
            "select a.title from Artwork a order by a.title limit 2 offset 2", String.class).getResultList();
        print(page);
      });

      step("23. Set operation: union");
      emf.runInTransaction(em -> {
        List<String> names = em.createQuery("""
            select ar.name from Artist ar where ar.country = 'France'
            union
            select g.name from Gallery g where g.floor = 0""", String.class).getResultList();
        print(names);
      });

      step("24. From-less select");
      emf.runInTransaction(em -> {
        Object value = em.createQuery("select 2 + 3").getSingleResult();
        print(value + " " + value.getClass().getSimpleName());
      });

      step("25. CTE with the with clause");
      emf.runInTransaction(em -> {
        List<String> titles = em.createQuery("""
            with expensive as (
              select a.title as title, a.estimatedValue as price
              from Artwork a where a.estimatedValue > 1000
            )
            select e.title from expensive e order by e.price desc""", String.class).getResultList();
        print(titles);
      });

      step("26. Bulk update with createMutationQuery");
      emf.runInTransaction(em -> {
        Artwork train = em.createQuery("from Artwork a where a.title = 'Night Train'", Artwork.class)
            .getSingleResult();
        int updated = em.unwrap(Session.class).createMutationQuery(
            "update Artwork a set a.estimatedValue = a.estimatedValue * 1.1 where a.medium = :m")
            .setParameter("m", Medium.PHOTO)
            .executeUpdate();
        print("updated " + updated);
        print("loaded entity: " + train.getEstimatedValue());
        em.refresh(train);
        print("after refresh: " + train.getEstimatedValue());
      });

      step("27. insert ... values");
      emf.runInTransaction(em -> {
        int inserted = em.unwrap(Session.class).createMutationQuery(
            "insert into Artist (name, country) values ('Ravi', 'India'), ('Sara', 'Italy')")
            .executeUpdate();
        print("inserted " + inserted);
      });

      step("28. insert ... select");
      emf.runInTransaction(em -> {
        int copied = em.unwrap(Session.class).createMutationQuery("""
            insert into Highlight (title, artistName)
            select a.title, a.artist.name from Artwork a where a.estimatedValue > 2000""")
            .executeUpdate();
        print("copied " + copied);
        print(em.createQuery("from Highlight h order by h.title", Highlight.class).getResultList());
      });

      step("29. Bulk delete");
      emf.runInTransaction(em -> {
        int deleted = em.createQuery("delete from Artwork a where a.gallery is null").executeUpdate();
        print("deleted " + deleted);
      });

      step("30. Table name instead of entity name");
      try {
        emf.runInTransaction(em -> em.createQuery("select a from artwork a", Artwork.class).getResultList());
      } catch (RuntimeException e) {
        print(e.getClass().getName() + ": " + e.getMessage());
      }
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title);
  }

  private static void print(Object value) {
    System.out.println("  -> " + value);
  }
}
