package com.howtodoinjava.hibernate.procedures;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import org.testcontainers.mysql.MySQLContainer;

public class StoredProcedureDemo {

  public static void main(String[] args) {
    try (MySQLContainer mysql = Database.startMySql();
         EntityManagerFactory emf = Database.create(mysql, true)) {

      Long javaId = emf.callInTransaction(em -> {
        Course java = new Course("Java Basics", "beginner", new BigDecimal("40.00"));
        em.persist(java);
        em.persist(new Course("SQL Basics", "beginner", new BigDecimal("30.00")));
        em.persist(new Course("Hibernate Internals", "advanced", new BigDecimal("80.00")));
        return java.getId();
      });

      System.out.println("\n== 1. OUT parameter ==");
      emf.runInTransaction(em ->
          System.out.println("beginner courses: " + CourseProcedures.countByLevel(em, "beginner")));

      System.out.println("\n== 2. Result set mapped to Course ==");
      emf.runInTransaction(em ->
          CourseProcedures.findByLevel(em, "beginner").forEach(System.out::println));

      System.out.println("\n== 3. Update with IN and OUT parameters ==");
      emf.runInTransaction(em ->
          System.out.println("new price: " + CourseProcedures.applyDiscount(em, javaId, 25)));

      System.out.println("\n== 4. Named stored procedure ==");
      emf.runInTransaction(em ->
          CourseProcedures.findByLevelNamed(em, "advanced").forEach(System.out::println));

      System.out.println("\n== 5. Hibernate ProcedureCall ==");
      emf.runInTransaction(em ->
          System.out.println("advanced courses: " + CourseProcedures.countByLevelHibernate(em, "advanced")));
    }
  }
}
