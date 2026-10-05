package com.howtodoinjava.hibernate.firstlevelcache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FirstLevelCacheTest {

  private EntityManagerFactory emf;
  private Long inceptionId;
  private Long upId;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    List<Long> ids = Database.saveMovies(emf);
    inceptionId = ids.get(0);
    upId = ids.get(1);
    SqlCounter.reset();
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  @Test
  void quickReference() {
    emf.runInTransaction(em -> {
      Movie first = em.find(Movie.class, 1L);
      assertEquals(1, SqlCounter.selects());
      Movie second = em.find(Movie.class, 1L);
      assertEquals(1, SqlCounter.selects());
      assertSame(first, second);
      em.detach(first);
      Movie third = em.find(Movie.class, 1L);
      assertNotSame(first, third);
      assertEquals(2, SqlCounter.selects());
    });
  }

  @Test
  void moviesGetIdsOneTwoThree() {
    assertEquals(1L, inceptionId);
    assertEquals(2L, upId);
  }

  @Test
  void findTwiceInOneEntityManagerRunsOneSelect() {
    emf.runInTransaction(em -> {
      Movie first = em.find(Movie.class, inceptionId);
      Movie second = em.find(Movie.class, inceptionId);
      assertSame(first, second);
      assertTrue(em.contains(first));
      assertEquals("Inception", second.getTitle());
    });
    assertEquals(1, SqlCounter.selects());
    assertTrue(SqlCounter.statements().get(0).startsWith(
        "select m1_0.id,m1_0.rating,m1_0.releaseYear,m1_0.title from Movie m1_0 where m1_0.id=?"));
  }

  @Test
  void twoEntityManagersHaveSeparateCaches() {
    try (EntityManager em1 = emf.createEntityManager();
         EntityManager em2 = emf.createEntityManager()) {
      Movie a = em1.find(Movie.class, inceptionId);
      Movie b = em2.find(Movie.class, inceptionId);
      assertNotSame(a, b);
      assertFalse(em2.contains(a));
      assertTrue(em1.contains(a));
    }
    assertEquals(2, SqlCounter.selects());
  }

  @Test
  void closedEntityManagerLosesItsCache() {
    emf.runInTransaction(em -> em.find(Movie.class, inceptionId));
    emf.runInTransaction(em -> em.find(Movie.class, inceptionId));
    assertEquals(2, SqlCounter.selects());
  }

  @Test
  void persistPutsTheEntityInTheCache() {
    emf.runInTransaction(em -> {
      Movie soul = new Movie("Soul", 2020, 8.0);
      em.persist(soul);
      assertSame(soul, em.find(Movie.class, soul.getId()));
      assertEquals(0, SqlCounter.count());
    });
    assertEquals(1, SqlCounter.count());
    assertTrue(SqlCounter.statements().get(0).startsWith("insert into Movie"));
  }

  @Test
  void queryAlwaysRunsSqlButReturnsTheCachedInstance() {
    emf.runInTransaction(em -> {
      Movie found = em.find(Movie.class, inceptionId);
      Movie queried = em.createQuery("from Movie where title = :title", Movie.class)
          .setParameter("title", "Inception").getSingleResult();
      em.createQuery("from Movie where title = :title", Movie.class)
          .setParameter("title", "Inception").getSingleResult();
      assertSame(found, queried);
    });
    assertEquals(3, SqlCounter.selects());
  }

  @Test
  void findAfterQueryUsesTheCache() {
    emf.runInTransaction(em -> {
      List<Movie> all = em.createQuery("from Movie order by id", Movie.class).getResultList();
      Movie up = em.find(Movie.class, upId);
      assertSame(all.get(1), up);
      assertEquals("[Inception (2010, 8.8), Up (2009, 8.3), Coco (2017, 8.4)]", all.toString());
    });
    assertEquals(1, SqlCounter.selects());
  }

  @Test
  void bulkUpdateLeavesCachedEntityStaleUntilRefresh() {
    emf.runInTransaction(em -> {
      Movie inception = em.find(Movie.class, inceptionId);
      int rows = em.createQuery("update Movie set rating = 9.0 where id = :id")
          .setParameter("id", inceptionId).executeUpdate();
      assertEquals(1, rows);
      assertEquals(8.8, em.find(Movie.class, inceptionId).getRating());
      Movie queried = em.createQuery("from Movie where id = :id", Movie.class)
          .setParameter("id", inceptionId).getSingleResult();
      assertSame(inception, queried);
      assertEquals(8.8, queried.getRating());
      em.refresh(inception);
      assertEquals(9.0, inception.getRating());
    });
    // find, update, query, refresh
    assertEquals(4, SqlCounter.count());
    assertTrue(SqlCounter.statements().get(1).startsWith("update Movie m1_0 set rating=9.0 where m1_0.id=?"));
  }

  @Test
  void detachRemovesOneEntity() {
    emf.runInTransaction(em -> {
      Movie inception = em.find(Movie.class, inceptionId);
      Movie up = em.find(Movie.class, upId);
      em.detach(inception);
      assertFalse(em.contains(inception));
      assertTrue(em.contains(up));
      Movie again = em.find(Movie.class, inceptionId);
      assertNotSame(inception, again);
    });
    assertEquals(3, SqlCounter.selects());
  }

  @Test
  void evictWorksLikeDetach() {
    emf.runInTransaction(em -> {
      Session session = em.unwrap(Session.class);
      Movie up = em.find(Movie.class, upId);
      session.evict(up);
      assertFalse(session.contains(up));
      em.find(Movie.class, upId);
    });
    assertEquals(2, SqlCounter.selects());
  }

  @Test
  void clearRemovesAllEntities() {
    emf.runInTransaction(em -> {
      Session session = em.unwrap(Session.class);
      Movie inception = em.find(Movie.class, inceptionId);
      em.find(Movie.class, upId);
      assertEquals(2, session.getStatistics().getEntityCount());
      em.clear();
      assertEquals(0, session.getStatistics().getEntityCount());
      assertFalse(em.contains(inception));
    });
  }

  @Test
  void clearDropsUnflushedChanges() {
    emf.runInTransaction(em -> {
      em.find(Movie.class, upId).setTitle("Up!");
      em.clear();
    });
    assertEquals(1, SqlCounter.count());
    assertEquals("Up", title(upId));
  }

  @Test
  void flushBeforeClearKeepsChanges() {
    emf.runInTransaction(em -> {
      em.find(Movie.class, upId).setTitle("Up!");
      em.flush();
      em.clear();
    });
    assertEquals(2, SqlCounter.count());
    assertTrue(SqlCounter.statements().get(1).startsWith("update Movie set rating=?,releaseYear=?,title=? where id=?"));
    assertEquals("Up!", title(upId));
  }

  @Test
  void changesToDetachedEntityAreNotSaved() {
    emf.runInTransaction(em -> {
      Movie up = em.find(Movie.class, upId);
      em.detach(up);
      up.setTitle("Up!");
    });
    assertEquals("Up", title(upId));
  }

  @Test
  void contextGrowsWithoutClear() {
    assertEquals(1000, FirstLevelCacheDemo.insertMovies(emf, 1000, 0));
  }

  @Test
  void clearEvery50KeepsContextSmall() {
    assertEquals(50, FirstLevelCacheDemo.insertMovies(emf, 1000, 50));
    long saved = emf.callInTransaction(em ->
        em.createQuery("select count(*) from Movie", Long.class).getSingleResult());
    assertEquals(1003, saved);
  }

  @Test
  void getReferenceReturnsCachedInstance() {
    emf.runInTransaction(em -> {
      Movie found = em.find(Movie.class, inceptionId);
      assertSame(found, em.getReference(Movie.class, inceptionId));
    });
    assertEquals(1, SqlCounter.selects());
  }

  @Test
  void getReferenceRunsNoSqlOnMiss() {
    emf.runInTransaction(em -> {
      Movie ref = em.getReference(Movie.class, upId);
      assertEquals(0, SqlCounter.count());
      assertTrue(em.contains(ref));
    });
  }

  @Test
  void refreshOnDetachedEntityThrows() {
    emf.runInTransaction(em -> {
      Movie up = em.find(Movie.class, upId);
      em.detach(up);
      assertThrows(IllegalArgumentException.class, () -> em.refresh(up));
    });
  }

  @Test
  void refreshOverwritesUnflushedChanges() {
    emf.runInTransaction(em -> {
      Movie up = em.find(Movie.class, upId);
      up.setTitle("Up!");
      em.refresh(up);
      assertEquals("Up", up.getTitle());
    });
  }

  @Test
  void flushEvery50SendsOneInsertBatch() {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.clear();
    FirstLevelCacheDemo.insertMovies(emf, 1000, 50);
    // 1000 rows, but the insert statement is prepared once per flush: 20 JDBC batches
    assertEquals(1000, stats.getEntityInsertCount());
    assertEquals(20, SqlCounter.statements().stream().filter(x -> x.startsWith("insert")).count());
  }

  @Test
  void statelessSessionHasNoCache() {
    SessionFactory sf = emf.unwrap(SessionFactory.class);
    try (StatelessSession stateless = sf.openStatelessSession()) {
      Movie a = stateless.get(Movie.class, inceptionId);
      Movie b = stateless.get(Movie.class, inceptionId);
      assertNotSame(a, b);
    }
    assertEquals(2, SqlCounter.selects());
  }

  @Test
  void statisticsCountOneLoadForFiveFinds() {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.clear();
    emf.runInTransaction(em -> {
      for (int i = 0; i < 5; i++) {
        em.find(Movie.class, inceptionId);
      }
    });
    assertEquals(1, stats.getEntityLoadCount());
    assertEquals(1, stats.getPrepareStatementCount());
  }

  private String title(Long id) {
    return emf.callInTransaction(em -> em.find(Movie.class, id).getTitle());
  }
}
