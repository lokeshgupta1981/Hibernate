package com.howtodoinjava.basics;

import com.howtodoinjava.basics.entity.EmployeeEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class SessionFactoryTest {

  @Test
  void createSessionFactoryWithoutXML() {

    Map<String, Object> settings = new HashMap<>();
    settings.put("hibernate.connection.driver_class", "org.h2.Driver");
    settings.put("hibernate.connection.url", "jdbc:h2:mem:test");
    settings.put("hibernate.connection.username", "sa");
    settings.put("hibernate.connection.password", "");
    settings.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
    settings.put("hibernate.current_session_context_class", "thread");
    settings.put("hibernate.show_sql", "true");
    settings.put("hibernate.format_sql", "true");
    settings.put("hibernate.hbm2ddl.auto", "create-drop");

    SessionFactory sessionFactory = null;
    Session session = null;

    try {
      ServiceRegistry standardRegistry = new StandardServiceRegistryBuilder()
          .applySettings(settings).build();

      Metadata metadata = new MetadataSources(standardRegistry)
          .addAnnotatedClass(EmployeeEntity.class)
          .getMetadataBuilder()
          .build();

      sessionFactory = metadata.getSessionFactoryBuilder().build();

    } catch (Throwable ex) {
      throw new ExceptionInInitializerError(ex);
    }

    session = sessionFactory.openSession();
    session.beginTransaction();

    EmployeeEntity emp = new EmployeeEntity();
    emp.setEmail("demo-user@mail.com");
    emp.setFirstName("demo");
    emp.setLastName("user");

    Assertions.assertNull(emp.getEmployeeId());

    session.persist(emp);

    Assertions.assertNotNull(emp.getEmployeeId());

    session.getTransaction().commit();
    sessionFactory.close();
  }

  @Test
  void createSessionFactoryWithXML() {
    SessionFactory sessionFactory = null;
    Session session = null;

    try {
      StandardServiceRegistry standardRegistry
          = new StandardServiceRegistryBuilder().configure()
          .build();

      Metadata metadata = new MetadataSources(standardRegistry)
          .getMetadataBuilder()
          .build();

      sessionFactory = metadata.getSessionFactoryBuilder().build();

    } catch (Throwable ex) {
      throw new ExceptionInInitializerError(ex);
    }

    session = sessionFactory.openSession();
    session.beginTransaction();

    EmployeeEntity emp = new EmployeeEntity();
    emp.setEmail("demo-user@mail.com");
    emp.setFirstName("demo");
    emp.setLastName("user");

    Assertions.assertNull(emp.getEmployeeId());

    session.persist(emp);

    Assertions.assertNotNull(emp.getEmployeeId());

    session.getTransaction().commit();
    sessionFactory.close();
  }
}
