package com.howtodoinjava.demo.mappings.manytomany;

import com.howtodoinjava.hibernate.manyToMany.joinTable.ReaderEntity;
import com.howtodoinjava.hibernate.manyToMany.joinTable.SubscriptionEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.Set;

public class TestManyToManyJoinTableAssociation {
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
          .addAnnotatedClass(ReaderEntity.class)
          .addAnnotatedClass(SubscriptionEntity.class)
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
    // Add subscription
    SubscriptionEntity subOne = new SubscriptionEntity();
    subOne.setSubscriptionName("Entertainment");

    SubscriptionEntity subTwo = new SubscriptionEntity();
    subTwo.setSubscriptionName("Horror");

    Set<SubscriptionEntity> subs = new HashSet<SubscriptionEntity>();
    subs.add(subOne);
    subs.add(subTwo);

    // Add readers
    ReaderEntity readerOne = new ReaderEntity();
    readerOne.setEmail("demo-user1@mail.com");
    readerOne.setFirstName("demo");
    readerOne.setLastName("user");

    ReaderEntity readerTwo = new ReaderEntity();
    readerTwo.setEmail("demo-user2@mail.com");
    readerTwo.setFirstName("demo");
    readerTwo.setLastName("user");

    Set<ReaderEntity> readers = new HashSet<ReaderEntity>();
    readers.add(readerOne);
    readers.add(readerTwo);

    readerOne.setSubscriptions(subs);
    readerTwo.setSubscriptions(subs);

    session.persist(readerOne);
    session.persist(readerTwo);
  }
}
