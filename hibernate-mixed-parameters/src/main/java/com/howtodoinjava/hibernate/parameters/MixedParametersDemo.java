package com.howtodoinjava.hibernate.parameters;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import java.util.function.Function;

public class MixedParametersDemo {

  private static EntityManagerFactory emf;

  public static void main(String[] args) {
    emf = Database.create(true);
    try {
      storedProcedures();
      nativeQueries();
      hqlQueries();
      colonsInNativeSql();
      otherParameterMistakes();
    } finally {
      emf.close();
    }

    step("HQL with strict JPA compliance (hibernate.jpa.compliance.query = true)");
    try (EntityManagerFactory strict = Database.create(true, true)) {
      emf = strict;
      run("named + ordinal", em -> em.createQuery(
              "from TrainService t where t.fromStation = :from and t.toStation = ?1", TrainService.class)
          .setParameter("from", "Delhi")
          .setParameter(1, "Mumbai")
          .getResultList());
    }
  }

  static void storedProcedures() {
    step("1. Stored procedure: register by name AND by position");
    run("named, then positional", em -> em.createStoredProcedureQuery("count_departures")
        .registerStoredProcedureParameter("p_from", String.class, ParameterMode.IN)
        .registerStoredProcedureParameter(2, Integer.class, ParameterMode.IN));

    step("2. Stored procedure: String and Integer variables passed instead of names");
    run("String and Integer variables", em -> {
      String from = "Delhi";
      Integer hour = 7;
      return em.createStoredProcedureQuery("count_departures")
          .registerStoredProcedureParameter(from, String.class, ParameterMode.IN)
          .registerStoredProcedureParameter(hour, Integer.class, ParameterMode.IN);
    });

    step("3. Stored procedure: names only");
    run("names", em -> em.createStoredProcedureQuery("count_departures")
        .registerStoredProcedureParameter("p_from", String.class, ParameterMode.IN)
        .registerStoredProcedureParameter("p_hour", Integer.class, ParameterMode.IN)
        .setParameter("p_from", "Delhi")
        .setParameter("p_hour", 7)
        .getSingleResult());

    step("4. Stored procedure: positions only");
    run("positions", em -> em.createStoredProcedureQuery("count_departures")
        .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
        .registerStoredProcedureParameter(2, Integer.class, ParameterMode.IN)
        .setParameter(1, "Delhi")
        .setParameter(2, 7)
        .getSingleResult());
  }

  static void nativeQueries() {
    step("5. Native query: named + JDBC-style ?");
    run(":from and ?", em -> em.createNativeQuery(
        "select * from TrainService where fromStation = :from and toStation = ?", TrainService.class));

    step("6. Native query: ordinal ?1 + JDBC-style ?");
    run("?1 and ?", em -> em.createNativeQuery(
        "select * from TrainService where fromStation = ?1 and toStation = ?", TrainService.class));

    step("7. Native query: named + ordinal ?1 (accepted by Hibernate)");
    run(":from and ?1", em -> em.createNativeQuery(
            "select * from TrainService where fromStation = :from and toStation = ?1", TrainService.class)
        .setParameter("from", "Delhi")
        .setParameter(1, "Mumbai")
        .getResultList());

    step("8. Native query: named only");
    run(":from and :to", em -> em.createNativeQuery(
            "select * from TrainService where fromStation = :from and toStation = :to", TrainService.class)
        .setParameter("from", "Delhi")
        .setParameter("to", "Mumbai")
        .getResultList());

    step("9. Native query: JDBC-style ? only");
    run("? and ?", em -> em.createNativeQuery(
            "select * from TrainService where fromStation = ? and toStation = ?", TrainService.class)
        .setParameter(1, "Delhi")
        .setParameter(2, "Mumbai")
        .getResultList());
  }

  static void hqlQueries() {
    step("10. HQL: named + ordinal ?1 (default settings)");
    run(":from and ?1", em -> em.createQuery(
            "from TrainService t where t.fromStation = :from and t.toStation = ?1", TrainService.class)
        .setParameter("from", "Delhi")
        .setParameter(1, "Mumbai")
        .getResultList());

    step("11. HQL: JDBC-style ?");
    run("?", em -> em.createQuery(
        "from TrainService t where t.fromStation = ?", TrainService.class));

    step("12. HQL: named + JDBC-style ?");
    run(":from and ?", em -> em.createQuery(
        "from TrainService t where t.fromStation = :from and t.toStation = ?", TrainService.class));

    step("13. HQL: named only, one name used twice");
    run(":city twice", em -> em.createQuery(
            "from TrainService t where t.fromStation = :city or t.toStation = :city", TrainService.class)
        .setParameter("city", "Mumbai")
        .getResultList());
  }

  static void colonsInNativeSql() {
    step("14. PostgreSQL-style cast right after a parameter");
    run(":from::varchar", em -> em.createNativeQuery(
            "select trainNumber from TrainService where fromStation = :from::varchar")
        .setParameter("from", "Delhi"));

    step("15. Fix: CAST(... AS ...)");
    run("cast(:from as varchar)", em -> em.createNativeQuery(
            "select trainNumber from TrainService where fromStation = cast(:from as varchar)")
        .setParameter("from", "Delhi")
        .getResultList());

    step("16. Fix: escape the colons");
    run(":from\\:\\:varchar", em -> em.createNativeQuery(
            "select trainNumber from TrainService where fromStation = :from\\:\\:varchar")
        .setParameter("from", "Delhi")
        .getResultList());

    step("17. A cast on a column, colons inside a string literal");
    run("departsAt::varchar and '06:00'", em -> em.createNativeQuery(
            "select departsAt::varchar from TrainService where departsAt > '06:00:00' and fromStation = :from")
        .setParameter("from", "Delhi")
        .getResultList());

    step("18. JSON key and value with no space after the colon");
    run("'from':fromStation", em -> em.createNativeQuery(
            "select cast(json_object('from':fromStation) as varchar) from TrainService where trainNumber = :number")
        .setParameter("number", 101)
        .getResultList());

    step("19. Fix: escape the colon");
    run("'from'\\:fromStation", em -> em.createNativeQuery(
            "select cast(json_object('from'\\:fromStation) as varchar) from TrainService where trainNumber = :number")
        .setParameter("number", 101)
        .getResultList());
  }

  static void otherParameterMistakes() {
    step("20. Setting a parameter the query does not have");
    run("setParameter(\"station\")", em -> em.createQuery(
            "from TrainService t where t.fromStation = :from", TrainService.class)
        .setParameter("station", "Delhi"));

    step("21. Positions start at 1: setParameter(0, ...)");
    run("setParameter(0)", em -> em.createQuery(
            "from TrainService t where t.fromStation = ?1", TrainService.class)
        .setParameter(0, "Delhi"));

    step("22. Positions start at 1: ?0 in the query");
    run("?0", em -> em.createQuery(
        "from TrainService t where t.fromStation = ?0", TrainService.class));
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title);
  }

  private static void run(String label, Function<EntityManager, Object> work) {
    try {
      Object result = emf.callInTransaction(work::apply);
      if (result instanceof java.util.List<?> || result instanceof Number) {
        System.out.println(label + " -> " + result);
      } else {
        System.out.println(label + " -> accepted");
      }
    } catch (RuntimeException e) {
      System.out.println(label + " -> " + e.getClass().getName() + ": " + e.getMessage());
      for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
        System.out.println("  Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
      }
    }
  }
}
