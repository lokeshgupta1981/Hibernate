package com.howtodoinjava.demo;

import com.howtodoinjava.demo.entity.EmployeeEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

public class TestDeleteEntity {
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
    em.getTransaction().commit();
    em.close();
  }

  @Test
  void testNormalDeleteJPA() {
    EmployeeEntity emp = new EmployeeEntity();
    emp.setEmail("demo-user@mail.com");
    emp.setFirstName("demo");
    emp.setLastName("user");

    Assertions.assertNull(emp.getEmployeeId());

    em.persist(emp);
    flushAndClear();

    Assertions.assertNotNull(emp.getEmployeeId());

    EmployeeEntity employee = em.find(EmployeeEntity.class,
        emp.getEmployeeId());

    //IllegalArgumentException Removing a detached instance com.howtodoinjava
    // .demo.entity.EmployeeEntity#1
    //em.detach(employee);

    em.remove(employee);

    flushAndClear();
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }
}
