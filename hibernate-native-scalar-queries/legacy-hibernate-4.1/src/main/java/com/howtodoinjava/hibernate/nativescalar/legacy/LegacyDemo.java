package com.howtodoinjava.hibernate.nativescalar.legacy;

import com.howtodoinjava.hibernate.nativescalar.legacy.fixed.Sighting;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class LegacyDemo {

  public static void main(String[] args) {
    System.out.println("== 1. Unit with a scalar named native query and no mapping");
    try {
      EntityManagerFactory broken = Persistence.createEntityManagerFactory("birds-broken");
      Object robins = broken.createEntityManager().createNamedQuery("Sighting.countBySpecies")
          .setParameter(1, "Robin").getSingleResult();
      System.out.println("Started without errors, countBySpecies(Robin) = " + robins
          + " (" + robins.getClass().getName() + ")");
      broken.close();
    } catch (RuntimeException e) {
      Throwable t = e;
      while (t != null) {
        System.out.println(t.getClass().getName() + ": " + t.getMessage());
        t = t.getCause();
      }
      e.printStackTrace(System.out);
    }

    System.out.println("== 2. Unit with a native UPDATE as a named query and no mapping");
    try {
      Persistence.createEntityManagerFactory("birds-broken-update").close();
      System.out.println("Started without errors");
    } catch (RuntimeException e) {
      System.out.println(e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
    }

    System.out.println("== 3. Unit with @SqlResultSetMapping and @ColumnResult");
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("birds-fixed");
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    em.persist(new Sighting("Robin", "Lake Park", 3, Date.valueOf("2026-09-20")));
    em.persist(new Sighting("Heron", "River Bend", 1, Date.valueOf("2026-09-21")));
    em.persist(new Sighting("Robin", "River Bend", 2, Date.valueOf("2026-09-22")));
    em.flush();

    Object robins = em.createNamedQuery("Sighting.countBySpecies")
        .setParameter(1, "Robin").getSingleResult();
    System.out.println("countBySpecies(Robin) = " + robins + " (" + robins.getClass().getName() + ")");

    List<?> totals = em.createNamedQuery("Sighting.totalsBySpecies").getResultList();
    for (Object row : totals) {
      Object[] r = (Object[]) row;
      System.out.println("totalsBySpecies row = " + Arrays.toString(r) + " (" + r[1].getClass().getName() + ")");
    }

    int updated = em.createNamedQuery("Sighting.renameLocation")
        .setParameter(1, "River Walk").setParameter(2, "River Bend").executeUpdate();
    System.out.println("renameLocation updated " + updated + " rows");
    em.getTransaction().commit();
    em.close();
    emf.close();
  }
}
