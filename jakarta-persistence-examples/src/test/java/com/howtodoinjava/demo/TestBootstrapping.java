package com.howtodoinjava.demo;

import com.howtodoinjava.demo.entity.EmployeeEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

public class TestBootstrapping {

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
  }

  @AfterEach
  void tearThis() {
    em.close();
  }
  
  @Test
  void createSessionFactoryWithXML() {
    EmployeeEntity emp = new EmployeeEntity();
    emp.setEmail("demo-user@mail.com");
    emp.setFirstName("demo");
    emp.setLastName("user");

    Assertions.assertNull(emp.getEmployeeId());

    em.getTransaction().begin();
    em.persist(emp);
    em.getTransaction().commit();

    Assertions.assertNotNull(emp.getEmployeeId());
  }
}
