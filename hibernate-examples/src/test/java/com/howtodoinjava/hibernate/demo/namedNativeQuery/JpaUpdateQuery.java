package com.howtodoinjava.hibernate.demo.namedNativeQuery;

import com.howtodoinjava.hibernate.demo.model.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JpaUpdateQuery {

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
  void testNamedNativeUpdateQuery() {
    EntityManager entityManager = emf.createEntityManager();
    Employee employee = new Employee("Lokesh", "Gupta", "lokesh@email.com", null);

    entityManager.getTransaction().begin();
    entityManager.persist(employee);
    entityManager.getTransaction().commit();

    entityManager.getTransaction().begin();
    // Set the named parameters
    String newFirstName = "John"; // New first name
    String newLastName = "Doe";  // New last name
    int employeeId = employee.getId();        // Employee ID to update

    // Create and execute the named native query
    Query updateQuery = entityManager.createNamedQuery("updateEmployeeName")
      .setParameter("newFirstName", newFirstName)
      .setParameter("newLastName", newLastName)
      .setParameter("employeeId", employeeId);

    int updatedCount = updateQuery.executeUpdate();
    entityManager.getTransaction().commit();

    Assertions.assertEquals(1, updatedCount);
  }
}
