package com.howtodoinjava.basics;

import com.howtodoinjava.basics.entity.EmployeeEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;

import java.util.List;

public class TestSecondLevelCache {

  private static SessionFactory sessionFactory = null;
  private Session session = null;

  @BeforeAll
  static void setup() {
    try {
      StandardServiceRegistry standardRegistry
        = new StandardServiceRegistryBuilder()
        .configure("hibernate-test.cfg.xml")
        .build();

      Metadata metadata = new MetadataSources(standardRegistry)
        .getMetadataBuilder()
        .build();

      sessionFactory = metadata
        .getSessionFactoryBuilder().build();

    } catch (Throwable ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @BeforeEach
  void setupThis() {
    session = sessionFactory.openSession();
    session.beginTransaction();
  }

  @AfterEach
  void tearThis() {
    session.getTransaction().commit();
  }

  @AfterAll
  static void tear() {
    sessionFactory.close();
  }

  @Test
  void testL2Cahce() {
    EmployeeEntity emp = new EmployeeEntity();
    emp.setEmail("demo-user@mail.com");
    emp.setFirstName("demo");
    emp.setLastName("user");

    Assertions.assertNull(emp.getEmployeeId());

    session.persist(emp);

    Assertions.assertNotNull(emp.getEmployeeId());

    session.flush();
    session.close();

    //New Session

    session = sessionFactory.openSession();
    session.beginTransaction();

    session.get(EmployeeEntity.class, emp.getEmployeeId());

    Assertions.assertEquals(0,
      session.getSessionFactory()
        .getStatistics()
        .getSecondLevelCacheHitCount());

    session.flush();
    session.close();

    //New Session

    session = sessionFactory.openSession();
    session.beginTransaction();
    List<EmployeeEntity> emps = session.createQuery("SELECT a FROM EmployeeEntity a", EmployeeEntity.class).getResultList();

    session.get(EmployeeEntity.class, emp.getEmployeeId());

    Assertions.assertEquals(1,
      session.getSessionFactory()
        .getStatistics()
        .getSecondLevelCacheHitCount());

    session.flush();
    session.close();

    //New Session

    session = sessionFactory.openSession();
    session.beginTransaction();

    session.get(EmployeeEntity.class, emp.getEmployeeId());

    Assertions.assertEquals(2,
      session.getSessionFactory()
        .getStatistics()
        .getSecondLevelCacheHitCount());
  }
}
