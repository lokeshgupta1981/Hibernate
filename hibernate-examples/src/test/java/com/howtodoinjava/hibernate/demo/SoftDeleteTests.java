package com.howtodoinjava.hibernate.demo;

import com.howtodoinjava.hibernate.demo.entity.EmployeeEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
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
class SoftDeleteTests {

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

  //@Test
  void softDeleteWithDeletedColumn() {
    EmployeeEntity emp = new EmployeeEntity("Lokesh", "Gupta", "admin@howtodoinjava.com");

    EntityManager entityManager = emf.createEntityManager();

    entityManager.getTransaction().begin();
    entityManager.persist(emp);
    entityManager.getTransaction().commit();

    entityManager.getTransaction().begin();
    entityManager.remove(emp);
    entityManager.getTransaction().commit();

    List<EmployeeEntity> deletedEmployees = entityManager
      .createNamedQuery("getInactiveEmployees", EmployeeEntity.class)
      .getResultList();

    for (EmployeeEntity employee: deletedEmployees) {
      System.out.println(employee);
    }

    Assertions.assertEquals(1, deletedEmployees.size());
    Assertions.assertEquals(1, deletedEmployees.get(0).getEmployeeId());

    //@SoftDelete(strategy = SoftDeleteType.DELETED)
    /*Hibernate: select next_val as id_val from Employee_SEQ for update
    Hibernate: update Employee_SEQ set next_val= ? where next_val=?
    Hibernate: insert into Employee (email,firstName,lastName,deleted,ID) values (?,?,?,0,?)
    Hibernate: update Employee set deleted=1 where ID=? and deleted=0*/

    //@SoftDelete(strategy = SoftDeleteType.ACTIVE)
    /*Hibernate: select next_val as id_val from Employee_SEQ for update
    Hibernate: update Employee_SEQ set next_val= ? where next_val=?
    Hibernate: insert into Employee (email,firstName,lastName,active,ID) values (?,?,?,1,?)
    Hibernate: update Employee set active=0 where ID=? and active=1*/

    //@SoftDelete(converter = YesNoConverter.class)
    /*Hibernate: select next_val as id_val from Employee_SEQ for update
    Hibernate: update Employee_SEQ set next_val= ? where next_val=?
    Hibernate: insert into Employee (email,firstName,lastName,deleted,ID) values (?,?,?,'N',?)
    Hibernate: update Employee set deleted='Y' where ID=? and deleted='N'*/

    //@SoftDelete(converter = TrueFalseConverter.class)
    /*Hibernate: select next_val as id_val from Employee_SEQ for update
    Hibernate: update Employee_SEQ set next_val= ? where next_val=?
    Hibernate: insert into Employee (email,firstName,lastName,deleted,ID) values (?,?,?,'F',?)
    Hibernate: update Employee set deleted='T' where ID=? and deleted='F'*/

    //Custom Column
    //@SoftDelete(columnName = "ACT", strategy = SoftDeleteType.ACTIVE, converter = NumericBooleanConverter.class)
    /*Hibernate: select next_val as id_val from Employee_SEQ for update
    Hibernate: update Employee_SEQ set next_val= ? where next_val=?
    Hibernate: insert into Employee (email,firstName,lastName,ACT,ID) values (?,?,?,1,?)
    Hibernate: update Employee set ACT=0 where ID=? and ACT=1*/
  }

  @Test
  void softDeleteCollectionWithDeletedColumn() {
    EmployeeEntity emp = new EmployeeEntity("Amit", "Gupta", "amit@email.com");
    emp.setPhoneNumbers(List.of("111111", "222222", "333333"));

    EntityManager entityManager = emf.createEntityManager();

    entityManager.getTransaction().begin();
    entityManager.persist(emp);
    entityManager.getTransaction().commit();

    entityManager.getTransaction().begin();
    emp.getPhoneNumbers().remove("111111");
    entityManager.getTransaction().commit();
  }
}
