package com.howtodoinjava.demo.mappings.onetotone;

import com.howtodoinjava.hibernate.onetoone.dto.sharedPrimaryKey.AccountEntity;
import com.howtodoinjava.hibernate.onetoone.dto.sharedPrimaryKey.EmployeeEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;

public class TestSharedPrimaryKeyAssociation {
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
          .addAnnotatedClass(AccountEntity.class)
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
  void testMapping() {
    AccountEntity account = new AccountEntity();
    account.setAccountNumber("123-345-65454");

    // Add new Employee object
    EmployeeEntity emp = new EmployeeEntity();
    emp.setEmail("demo-user@mail.com");
    emp.setFirstName("demo");
    emp.setLastName("user");

    // Save Account
    session.persist(account);
    Assertions.assertNotNull(account.getAccountId());

    // Save Employee
    emp.setAccount(account);
    session.persist(emp);
    Assertions.assertNotNull(emp.getEmployeeId());
    Assertions.assertNotNull(emp.getAccount().getAccountId());
  }
}
