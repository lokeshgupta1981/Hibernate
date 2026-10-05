package com.howtodoinjava.hibernate.springconfig;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * A DAO that keeps using the native Hibernate Session API on Spring Boot.
 * The Session comes from the transactional EntityManager, not from getCurrentSession().
 */
@Repository
public class ShowtimeDao {

  private final EntityManager entityManager;

  public ShowtimeDao(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Transactional
  public void save(Showtime showtime) {
    entityManager.unwrap(Session.class).persist(showtime);
  }

  @Transactional(readOnly = true)
  public List<Showtime> findByMovie(String movieTitle) {
    return entityManager.unwrap(Session.class)
        .createSelectionQuery("from Showtime where movieTitle = :title order by startsAt", Showtime.class)
        .setParameter("title", movieTitle)
        .getResultList();
  }
}
