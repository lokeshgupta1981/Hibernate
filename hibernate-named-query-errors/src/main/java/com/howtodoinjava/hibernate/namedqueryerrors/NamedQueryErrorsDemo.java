package com.howtodoinjava.hibernate.namedqueryerrors;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.query.NamedQueryValidationException;

public class NamedQueryErrorsDemo {

  public static void main(String[] args) {
    step("1. Fixed named queries start and run");
    try (EntityManagerFactory emf = Database.create(true)) {
      Database.seed(emf);
      emf.runInTransaction(em -> {
        List<Shift> longShifts = em.createNamedQuery("Shift.findLong", Shift.class)
            .setParameter("hours", 4)
            .getResultList();
        System.out.println("Shift.findLong(4) = " + longShifts);

        List<Shift> lokesh = em.createNamedQuery("Shift.findByVolunteer", Shift.class)
            .setParameter("name", "Lokesh")
            .getResultList();
        System.out.println("Shift.findByVolunteer(Lokesh) = " + lokesh);

        List<Shift> after = em.createNamedQuery("Shift.findAfter", Shift.class)
            .setParameter("from", LocalDateTime.of(2026, 10, 11, 0, 0))
            .getResultList();
        System.out.println("Shift.findAfter(2026-10-11) = " + after);

        List<Shift> sql = em.createNamedQuery("Shift.findLongSql", Shift.class)
            .setParameter("hours", 4)
            .getResultList();
        System.out.println("Shift.findLongSql(4) = " + sql);
      });
    }

    step("2. Each broken JPQL named query stops the startup");
    List<Class<?>> brokenClasses = List.of(BrokenQueries.TableName.class,
        BrokenQueries.ColumnName.class, BrokenQueries.AttributeTypo.class,
        BrokenQueries.UnregisteredEntity.class, BrokenQueries.SqlInJpql.class,
        BrokenQueries.KeywordTypo.class, BrokenQueries.TrailingSemicolon.class,
        BrokenQueries.UnlabeledParameter.class, BrokenQueries.ZeroParameter.class,
        BrokenQueries.TypeMismatch.class, BrokenQueries.TwoErrors.class);
    for (Class<?> broken : brokenClasses) {
      System.out.println("-- " + broken.getSimpleName());
      try {
        EntityManagerFactory emf = broken == BrokenQueries.UnregisteredEntity.class
            ? Database.base(false).managedClasses(Volunteer.class, broken)
                .createEntityManagerFactory()
            : Database.create(false, broken);
        emf.close();
        System.out.println("started");
      } catch (PersistenceException e) {
        System.out.println(e.getMessage());
        if (e.getCause() instanceof NamedQueryValidationException invalid) {
          Map<String, HibernateException> errors = invalid.getErrors();
          errors.forEach((name, error) -> System.out.println("  " + name + " -> " + error.getMessage()));
        }
      }
    }

    step("3. hibernate.query.startup_check = false moves the error to createNamedQuery()");
    try (EntityManagerFactory emf = Database.configuration(false, BrokenQueries.ColumnName.class)
        .property("hibernate.query.startup_check", false)
        .createEntityManagerFactory()) {
      System.out.println("factory started");
      emf.runInTransaction(em -> em.createNamedQuery("Shift.findStartingAfter", Shift.class));
    } catch (IllegalArgumentException e) {
      System.out.println(e);
    }

    step("4. A named native query is not checked at startup");
    try (EntityManagerFactory emf = Database.create(false, BrokenQueries.NativeWrongTable.class)) {
      System.out.println("factory started");
      emf.runInTransaction(em -> em.createNamedQuery("Shift.findAllSql", Shift.class).getResultList());
    } catch (PersistenceException e) {
      System.out.println(e);
    }

    step("5. The old HibernateUtil pattern");
    try {
      HibernateUtil.getSessionFactory();
    } catch (ExceptionInInitializerError e) {
      System.out.println("first call: " + e + " caused by " + e.getCause().getClass().getName());
    }
    try {
      HibernateUtil.getSessionFactory();
    } catch (NoClassDefFoundError e) {
      System.out.println("second call: " + e);
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title);
  }
}
