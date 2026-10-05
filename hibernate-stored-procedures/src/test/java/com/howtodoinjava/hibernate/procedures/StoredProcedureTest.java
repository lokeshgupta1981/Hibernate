package com.howtodoinjava.hibernate.procedures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.testcontainers.mysql.MySQLContainer;

class StoredProcedureTest {

  static MySQLContainer mysql;
  static EntityManagerFactory emf;
  Long javaId;

  @BeforeAll
  static void startDatabase() {
    mysql = Database.startMySql();
    emf = Database.create(mysql, false);
  }

  @AfterAll
  static void stopDatabase() {
    emf.close();
    mysql.stop();
  }

  @BeforeEach
  void insertCourses() {
    javaId = emf.callInTransaction(em -> {
      em.createQuery("delete from Course").executeUpdate();
      Course java = new Course("Java Basics", "beginner", new BigDecimal("40.00"));
      em.persist(java);
      em.persist(new Course("SQL Basics", "beginner", new BigDecimal("30.00")));
      em.persist(new Course("Hibernate Internals", "advanced", new BigDecimal("80.00")));
      return java.getId();
    });
  }

  @Test
  void readsOutParameter() {
    assertEquals(Integer.valueOf(2), emf.<Integer>callInTransaction(em -> CourseProcedures.countByLevel(em, "beginner")));
  }

  @Test
  void readsOutParameterByPosition() {
    assertEquals(Integer.valueOf(2), emf.<Integer>callInTransaction(em -> CourseProcedures.countByLevelPositional(em, "beginner")));
  }

  @Test
  void mapsResultSetToEntities() {
    List<Course> courses = emf.callInTransaction(em -> CourseProcedures.findByLevel(em, "beginner"));
    assertEquals(List.of("Java Basics", "SQL Basics"), courses.stream().map(Course::getTitle).toList());
  }

  @Test
  void returnsRowsAsObjectArrays() {
    List<Object[]> rows = emf.callInTransaction(em -> CourseProcedures.findRowsByLevel(em, "advanced"));
    assertEquals(1, rows.size());
    assertEquals(4, rows.get(0).length);
    assertEquals("Hibernate Internals", rows.get(0)[1]);
  }

  @Test
  void updatesDataAndReturnsOutParameter() {
    BigDecimal price = emf.callInTransaction(em -> CourseProcedures.applyDiscount(em, javaId, 25));
    assertEquals(new BigDecimal("30.00"), price);
    BigDecimal stored = emf.callInTransaction(em -> em.find(Course.class, javaId).getPrice());
    assertEquals(new BigDecimal("30.00"), stored);
  }

  @Test
  void loadedEntityIsStaleUntilRefresh() {
    List<BigDecimal> prices = emf.callInTransaction(em -> {
      Course java = em.find(Course.class, javaId);           // price 40.00 loaded
      CourseProcedures.applyDiscount(em, javaId, 25);         // database now has 30.00
      BigDecimal before = em.find(Course.class, javaId).getPrice();
      em.refresh(java);
      return List.of(before, java.getPrice());
    });
    assertEquals(List.of(new BigDecimal("40.00"), new BigDecimal("30.00")), prices);
  }

  @Test
  void callsNamedStoredProcedure() {
    List<Course> courses = emf.callInTransaction(em -> CourseProcedures.findByLevelNamed(em, "advanced"));
    assertEquals("Hibernate Internals", courses.get(0).getTitle());
  }

  @Test
  void callsWithHibernateProcedureCall() {
    assertEquals(Integer.valueOf(1), emf.<Integer>callInTransaction(em -> CourseProcedures.countByLevelHibernate(em, "advanced")));
  }

  @Test
  void passesNullParameter() {
    Integer total = emf.callInTransaction(em -> {
      StoredProcedureQuery query = em.createStoredProcedureQuery("count_courses_by_level")
          .registerStoredProcedureParameter("p_level", String.class, ParameterMode.IN)
          .registerStoredProcedureParameter("p_total", Integer.class, ParameterMode.OUT)
          .setParameter("p_level", null);
      query.execute();
      return (Integer) query.getOutputParameterValue("p_total");
    });
    assertEquals(0, total);
  }

  @Test
  void callsProcedureFromSpringDataRepository() {
    Integer total = emf.callInTransaction(em -> {
      CourseRepository repository = new JpaRepositoryFactory(em).getRepository(CourseRepository.class);
      return repository.countByLevel("beginner");
    });
    assertNotNull(total);
    assertEquals(2, total);
  }
}
