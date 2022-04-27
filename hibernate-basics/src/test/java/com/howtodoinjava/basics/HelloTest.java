package com.howtodoinjava.basics;

import com.howtodoinjava.basics.entity.EmployeeEntity;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;

public class HelloTest {

  private static SessionFactory sessionFactory = null;
  private Session session = null;


  @BeforeAll
  static void setup(){
    try {
      StandardServiceRegistry standardRegistry
          = new StandardServiceRegistryBuilder()
          .configure("hibernate-test.cfg.xml")
          .build();

      Metadata metadata = new MetadataSources(standardRegistry)
          .addAnnotatedClass(EmployeeEntity.class)
          .getMetadataBuilder()
          .build();

      sessionFactory = metadata
          .getSessionFactoryBuilder().build();

    } catch (Throwable ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @BeforeEach
  void setupThis(){
    session = sessionFactory.openSession();
    session.beginTransaction();
  }

  @AfterEach
  void tearThis(){
    session.getTransaction().commit();
  }

  @AfterAll
  static void tear(){
    sessionFactory.close();
  }

  @Test
  void createSessionFactoryWithXML() {
    EmployeeEntity emp = new EmployeeEntity();
    emp.setEmail("demo-user@mail.com");
    emp.setFirstName("demo");
    emp.setLastName("user");

    Assertions.assertNull(emp.getEmployeeId());

    session.persist(emp);

    Assertions.assertNotNull(emp.getEmployeeId());

    Hibernate.isInitialized(emp);
  }
}
