package com.howtodoinjava.hibernate.nativescalar.legacy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.howtodoinjava.hibernate.nativescalar.legacy.fixed.Sighting;
import java.math.BigInteger;
import java.sql.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import org.hibernate.cfg.NotYetImplementedException;
import org.junit.jupiter.api.Test;

class LegacyTest {

  @Test
  void scalarNamedNativeQueryWithoutMappingStopsStartup() {
    PersistenceException e = assertThrows(PersistenceException.class,
        () -> Persistence.createEntityManagerFactory("birds-broken"));
    assertEquals("[PersistenceUnit: birds-broken] Unable to build EntityManagerFactory", e.getMessage());
    Throwable cause = e.getCause();
    assertEquals(NotYetImplementedException.class, cause.getClass());
    assertEquals("Pure native scalar queries are not yet supported", cause.getMessage());
  }

  @Test
  void updateWithoutMappingFailsTheSameWay() {
    PersistenceException e = assertThrows(PersistenceException.class,
        () -> Persistence.createEntityManagerFactory("birds-broken-update"));
    assertEquals(NotYetImplementedException.class, e.getCause().getClass());
    assertEquals("Pure native scalar queries are not yet supported", e.getCause().getMessage());
  }

  @Test
  void columnResultMappingFixesIt() {
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("birds-fixed");
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    em.persist(new Sighting("Robin", "Lake Park", 3, Date.valueOf("2026-09-20")));
    em.persist(new Sighting("Heron", "River Bend", 1, Date.valueOf("2026-09-21")));
    em.persist(new Sighting("Robin", "River Bend", 2, Date.valueOf("2026-09-22")));
    em.flush();

    Object robins = em.createNamedQuery("Sighting.countBySpecies")
        .setParameter(1, "Robin").getSingleResult();
    assertEquals(BigInteger.valueOf(2), robins);

    List<?> totals = em.createNamedQuery("Sighting.totalsBySpecies").getResultList();
    assertArrayEquals(new Object[] {"Heron", BigInteger.valueOf(1)}, (Object[]) totals.get(0));
    assertArrayEquals(new Object[] {"Robin", BigInteger.valueOf(5)}, (Object[]) totals.get(1));

    int updated = em.createNamedQuery("Sighting.renameLocation")
        .setParameter(1, "River Walk").setParameter(2, "River Bend").executeUpdate();
    assertEquals(2, updated);
    em.getTransaction().rollback();
    em.close();
    emf.close();
  }

  @Test
  void adHocNativeQueryNeverNeededAMapping() {
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("birds-fixed");
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    em.persist(new Sighting("Robin", "Lake Park", 3, Date.valueOf("2026-09-20")));
    em.persist(new Sighting("Robin", "River Bend", 2, Date.valueOf("2026-09-22")));
    em.flush();

    Object robins = em.createNativeQuery("select count(*) from Sighting where species = ?")
        .setParameter(1, "Robin").getSingleResult();
    assertEquals(BigInteger.valueOf(2), robins);
    assertEquals(2L, ((Number) robins).longValue());
    em.getTransaction().rollback();
    em.close();
    emf.close();
  }
}
