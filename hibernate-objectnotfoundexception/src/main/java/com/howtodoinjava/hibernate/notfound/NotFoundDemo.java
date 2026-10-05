package com.howtodoinjava.hibernate.notfound;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class NotFoundDemo {

  static final String ORPHANS = """
      where fk(a.scholarship) is not null
        and not exists (select s from Scholarship s where s.id = fk(a.scholarship))""";

  public static void main(String[] args) {

    try (EntityManagerFactory emf = Database.create(ScholarshipApplication.class, true)) {
      step("0. Legacy data: Lokesh's application points to scholarship 99, which does not exist");
      Long meritId = Database.insertLegacyData(emf);

      step("1. getReference() of a missing id, then read a field");
      attempt(() -> emf.runInTransaction(em -> {
        Scholarship ref = em.getReference(Scholarship.class, 99L);
        System.out.println("class = " + ref.getClass().getSimpleName() + ", id = " + ref.getId());
        ref.getName();
      }));

      step("2. find() of a missing id");
      emf.runInTransaction(em -> System.out.println("find = " + em.find(Scholarship.class, 99L)));

      step("3. LAZY @ManyToOne pointing to the missing row");
      attempt(() -> emf.runInTransaction(em -> {
        ScholarshipApplication app = em.find(ScholarshipApplication.class, 1L);
        System.out.println("loaded " + app.getStudentName());
        app.getScholarship().getName();
      }));

      step("4. Check before use: find() the scholarship by the proxy's id");
      emf.runInTransaction(em -> {
        ScholarshipApplication app = em.find(ScholarshipApplication.class, 1L);
        Scholarship scholarship = em.find(Scholarship.class, app.getScholarship().getId());
        System.out.println("scholarship = " + scholarship);
      });

      step("5. List, then delete the orphaned applications");
      emf.runInTransaction(em -> {
        System.out.println("orphans = " + em.createQuery(
            "select a.studentName from ScholarshipApplication a " + ORPHANS, String.class)
            .getResultList());
        System.out.println("deleted = " + em.createQuery(
            "delete from ScholarshipApplication a " + ORPHANS).executeUpdate());
      });

      step("6. Row deleted by another transaction after the proxy was created");
      attempt(() -> emf.runInTransaction(em -> {
        ScholarshipApplication app = em.find(ScholarshipApplication.class, 2L);
        Scholarship merit = app.getScholarship();
        emf.runInTransaction(other ->
            other.createQuery("delete from Scholarship where id = :id")
                .setParameter("id", meritId).executeUpdate());
        merit.getName();
      }));
    }

    try (EntityManagerFactory emf = Database.create(EagerApplication.class, true)) {
      step("7. EAGER @ManyToOne pointing to the missing row: find()");
      Database.insertLegacyData(emf);
      attempt(() -> emf.runInTransaction(em -> em.find(EagerApplication.class, 1L)));

      step("8. EAGER @ManyToOne pointing to the missing row: query");
      attempt(() -> emf.runInTransaction(em ->
          em.createQuery("from ScholarshipApplication", EagerApplication.class).getResultList()));
    }

    try (EntityManagerFactory emf = Database.create(IgnoreApplication.class, true)) {
      step("9. @NotFound(IGNORE): the missing scholarship becomes null");
      Database.insertLegacyData(emf);
      emf.runInTransaction(em -> {
        IgnoreApplication app = em.find(IgnoreApplication.class, 1L);
        System.out.println("scholarship = " + app.getScholarship());
      });

      step("10. @NotFound(IGNORE): saving the application erases the old foreign key value");
      emf.runInTransaction(em -> em.find(IgnoreApplication.class, 1L).setStudentName("Lokesh Gupta"));
      emf.runInTransaction(em -> System.out.println("scholarship_id = " + em.createNativeQuery(
          "select scholarship_id from scholarship_application where id = 1").getSingleResult()));
    }

    try (EntityManagerFactory emf = Database.create(ExceptionApplication.class, true)) {
      step("11. @NotFound(EXCEPTION): fails while loading the application");
      Database.insertLegacyData(emf);
      attempt(() -> emf.runInTransaction(em -> em.find(ExceptionApplication.class, 1L)));
    }

    try (EntityManagerFactory emf = Database.create(CheckedApplication.class, true)) {
      step("12. Real foreign key constraint: the legacy insert is rejected");
      attempt(() -> Database.insertLegacyData(emf));
      attempt(() -> emf.runInTransaction(em -> em.persist(new CheckedApplication(
          "Lokesh", LocalDate.of(2026, 9, 15), em.getReference(Scholarship.class, 99L)))));
    }

    step("13. Native SessionFactory (no JPA bootstrap): ObjectNotFoundException");
    try (SessionFactory sf = new Configuration()
        .addAnnotatedClass(Scholarship.class)
        .setProperty("hibernate.connection.url", "jdbc:h2:mem:native;DB_CLOSE_DELAY=-1")
        .setProperty("hibernate.connection.username", "sa")
        .setProperty("hibernate.hbm2ddl.auto", "create-drop")
        .buildSessionFactory()) {
      attempt(() -> sf.inTransaction(session -> session.getReference(Scholarship.class, 99L).getName()));
    }
  }

  private static void attempt(Runnable action) {
    try {
      action.run();
      System.out.println("no exception");
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
      if (e.getCause() != null) {
        System.out.println("Caused by: " + e.getCause().getClass().getName());
      }
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title);
  }
}
