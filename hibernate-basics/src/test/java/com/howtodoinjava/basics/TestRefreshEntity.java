package com.howtodoinjava.basics;

import com.howtodoinjava.basics.entity.EmployeeEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestRefreshEntity {
  private static SessionFactory sessionFactory = null;

  @BeforeAll
  static void setup() {
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

  @AfterAll
  static void tear() {
    sessionFactory.close();
  }

  @Test
  void verifyDataIsRefreshed() {
    Session sessionOne = sessionFactory.openSession();
    Session sessionTwo = sessionFactory.openSession();

    sessionOne.getTransaction().begin();
    sessionTwo.getTransaction().begin();

    EmployeeEntity emp = new EmployeeEntity();
    emp.setEmail("demo-user@mail.com");
    emp.setFirstName("demo");
    emp.setLastName("user");

    //Persist in session 1
    sessionOne.persist(emp);
    Assertions.assertNotNull(emp.getEmployeeId());
  }
}
