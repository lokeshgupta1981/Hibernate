package com.howtodoinjava.basics;

import com.howtodoinjava.basics.entity.TransactionEntity;
import com.howtodoinjava.basics.entity.listners.AppIntegrator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;

import java.time.ZonedDateTime;

public class TestLifecycleEvents {

  private static SessionFactory sessionFactory = null;
  private Session session = null;


  @BeforeAll
  static void setup() {
    try {
      BootstrapServiceRegistryBuilder bootstrapRegistryBuilder =
          new BootstrapServiceRegistryBuilder();

      bootstrapRegistryBuilder.applyIntegrator(new AppIntegrator());

      BootstrapServiceRegistry bootstrapRegistry =
          bootstrapRegistryBuilder.build();

      StandardServiceRegistry standardRegistry
          = new StandardServiceRegistryBuilder(bootstrapRegistry)
          .configure("hibernate-test.cfg.xml")
          .build();

      Metadata metadata = new MetadataSources(standardRegistry)
          .addAnnotatedClass(TransactionEntity.class)
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
  void testPersist() {
    TransactionEntity transaction = new TransactionEntity();
    transaction.setAmount(100.0);
    transaction.setStartTS(ZonedDateTime.now());
    transaction.setEndTS(ZonedDateTime.now().plusSeconds(1));

    Assertions.assertNull(transaction.getId());
    session.persist(transaction);
    Assertions.assertNotNull(transaction.getId());

    session.getTransaction().commit();
    session.close();

    session = sessionFactory.openSession();
    session.getTransaction().begin();

    TransactionEntity loadedEntity = session.get(TransactionEntity.class,
        transaction.getId());
    Assertions.assertNotNull(loadedEntity.getId());
  }
}
