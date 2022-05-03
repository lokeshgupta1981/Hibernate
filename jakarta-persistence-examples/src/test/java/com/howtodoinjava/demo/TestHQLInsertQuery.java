package com.howtodoinjava.demo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

public class TestHQLInsertQuery {
  private static EntityManagerFactory emf;
  private EntityManager em;

  @BeforeAll
  static void setup() {
    emf = Persistence.createEntityManagerFactory("H2DB");
  }

  @AfterAll
  static void tear() {
    emf.close();
  }

  @BeforeEach
  void setupThis() {
    em = emf.createEntityManager();
    em.getTransaction().begin();
  }

  @AfterEach
  void tearThis() {
    em.close();
    em.getTransaction().commit();
  }

  @Test
  void helloWorld() {
    em.createQuery(
            "insert EmployeeEntity (employeeId, firstName, lastName, email) " +
                "values (1, 'Lokesh', 'Gupta', 'howtodoinjava@gmail.com')")
        .executeUpdate();
  }
}
