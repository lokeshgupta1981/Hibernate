package com.howtodoinjava.demo.mappings.onetomany;

import com.howtodoinjava.hibernate.oneToMany.joinTable.AccountEntity;
import com.howtodoinjava.hibernate.oneToMany.joinTable.EmployeeEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.Set;

public class TestOneToManyJoinTableAssociation {
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
  void testMapping() {
    AccountEntity account1 = new AccountEntity();
    account1.setAccountNumber("123-345-65454");

    AccountEntity account2 = new AccountEntity();
    account2.setAccountNumber("123-345-6542222");

    //Add new Employee object
    EmployeeEntity emp = new EmployeeEntity();
    emp.setEmail("demo-user@mail.com");
    emp.setFirstName("demo");
    emp.setLastName("user");

    Set<AccountEntity> accounts = new HashSet<>();
    accounts.add(account1);
    accounts.add(account2);

    emp.setAccounts(accounts);
    //Save Employee
    session.persist(emp);
  }
}
