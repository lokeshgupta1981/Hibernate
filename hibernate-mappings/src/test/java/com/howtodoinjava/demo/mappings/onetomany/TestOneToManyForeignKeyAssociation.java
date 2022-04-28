package com.howtodoinjava.demo.mappings.onetomany;

import com.howtodoinjava.hibernate.oneToMany.foreignKeyAsso.AccountEntity;
import com.howtodoinjava.hibernate.oneToMany.foreignKeyAsso.EmployeeEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.Set;

public class TestOneToManyForeignKeyAssociation {
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
    AccountEntity account1 = new AccountEntity();
    account1.setAccountNumber("Account detail 1");

    AccountEntity account2 = new AccountEntity();
    account2.setAccountNumber("Account detail 2");

    AccountEntity account3 = new AccountEntity();
    account3.setAccountNumber("Account detail 3");

    //Add new Employee object
    EmployeeEntity firstEmployee = new EmployeeEntity();
    firstEmployee.setEmail("demo-user-first@mail.com");
    firstEmployee.setFirstName("demo-one");
    firstEmployee.setLastName("user-one");

    EmployeeEntity secondEmployee = new EmployeeEntity();
    secondEmployee.setEmail("demo-user-second@mail.com");
    secondEmployee.setFirstName("demo-two");
    secondEmployee.setLastName("user-two");

    Set<AccountEntity> accountsOfFirstEmployee = new HashSet<AccountEntity>();
    accountsOfFirstEmployee.add(account1);
    accountsOfFirstEmployee.add(account2);

    Set<AccountEntity> accountsOfSecondEmployee = new HashSet<AccountEntity>();
    accountsOfSecondEmployee.add(account3);

    firstEmployee.setAccounts(accountsOfFirstEmployee);
    secondEmployee.setAccounts(accountsOfSecondEmployee);
    //Save Employee
    session.save(firstEmployee);
    session.save(secondEmployee);
  }
}
