package com.howtodoinjava.hibernate.demo;

import com.howtodoinjava.hibernate.demo.entity.EmployeeEntity;
import jakarta.persistence.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HibernateExamplesApplicationTests {

  private EntityManagerFactory emf;

  @Container
  private static final MySQLContainer MYSQL_CONTAINER = (MySQLContainer) new MySQLContainer()
    .withDatabaseName("testdb")
    .withUsername("root")
    .withPassword("password")
    .withReuse(true);

  @BeforeAll
  void setup() {
    System.setProperty("db.port", MYSQL_CONTAINER.getFirstMappedPort().toString());
    emf = Persistence.createEntityManagerFactory("mysql-persistence");
  }

  @Test
  void contextLoads() {
    EntityManager entityManager = emf.createEntityManager();
    entityManager.getTransaction().begin();

    entityManager.persist(new EmployeeEntity("Lokesh", "Gupta", "admin@howtodoinjava.com"));

    entityManager.getTransaction().commit();
  }
}
