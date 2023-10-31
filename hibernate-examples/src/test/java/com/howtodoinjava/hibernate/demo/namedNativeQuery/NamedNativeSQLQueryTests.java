package com.howtodoinjava.hibernate.demo.namedNativeQuery;

import com.howtodoinjava.hibernate.demo.entity.EmployeeEntity;
import com.howtodoinjava.hibernate.demo.model.Department;
import com.howtodoinjava.hibernate.demo.model.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NamedNativeSQLQueryTests {

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
  void testNamedNativeSelectQuery() {
    EntityManager entityManager = emf.createEntityManager();

    entityManager.getTransaction().begin();

    Department dept1 = new Department("HR");
    Department dept2 = new Department("Finance");
    entityManager.persist(dept1);
    entityManager.persist(dept2);

    entityManager.persist(new Employee("Lokesh", "Gupta", "lokesh@email.com", dept1));
    entityManager.persist(new Employee("Amit", "Gupta", "amit@email.com", dept1));
    entityManager.persist(new Employee("Govind", "Gupta", "govind@email.com", dept2));

    entityManager.getTransaction().commit();

    List<Employee> employeeList = entityManager.createNamedQuery("getEmployeesByDeptId", Employee.class)
      .setParameter("deptId", dept1.getId())
      .getResultList();

    Assertions.assertEquals(2, employeeList.size());
  }
}
