package com.howtodoinjava.basics;

import com.howtodoinjava.basics.entity.EmployeeEntity;
import com.howtodoinjava.basics.entity.interceptor.AuditInterceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestInterceptor {
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
          .getSessionFactoryBuilder()
          .applyInterceptor(new AuditInterceptor())
          .build();

    } catch (Throwable ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @Test
  public void testAuditLogging() {
    try (Session session = sessionFactory.withOptions()
        .interceptor(new AuditInterceptor()).openSession()) {
      session.getTransaction().begin();

      EmployeeEntity employee = new EmployeeEntity();
      employee.setFirstName("Lokesh");
      employee.setLastName("Gupta");
      employee.setEmail("howtodoinjava@gmail.com");

      //Save here
      session.persist(employee);
      session.flush();

      //Update here
      employee.setFirstName("Akash");

      session.getTransaction().commit();
    }
  }
}
