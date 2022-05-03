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

public class TestDeleteEntity {

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
  void testDeleteEntity() {
    EmployeeEntity emp = new EmployeeEntity();
    emp.setEmail("howtodoinjava@gmail.com");
    emp.setFirstName("demo");
    emp.setLastName("user");

    Assertions.assertNull(emp.getEmployeeId());

    beginSession();
    session.persist(emp);
    commitSession();

    beginSession();
    emp = session.get(EmployeeEntity.class, emp.getEmployeeId());
    session.evict(emp);

    session.remove(emp);
    commitSession();

    Assertions.assertNotNull(emp.getEmployeeId());
  }

  private void beginSession() {
    session = sessionFactory.openSession();
    session.getTransaction().begin();
  }

  private void commitSession() {
    session.getTransaction().commit();
    session.close();
  }
}
