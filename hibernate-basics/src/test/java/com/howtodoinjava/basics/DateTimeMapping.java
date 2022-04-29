package com.howtodoinjava.basics;

import com.howtodoinjava.basics.entity.ArticleEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateTimeMapping {

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
          .addAnnotatedClass(ArticleEntity.class)
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
    session.close();
  }

  @AfterAll
  static void tear() {
    sessionFactory.close();
  }

  @Test
  void testDateTimeMapping() {

    LocalDate date = LocalDate.of(1990, 1, 1);
    LocalTime time = LocalTime.of(10, 10);
    LocalDateTime timestamp = LocalDateTime.of(1990, 1, 1, 10, 10);

    ArticleEntity article = new ArticleEntity();
    article.setTitle("Title");
    article.setContent("Content");
    article.setLastUpdateDate(date);
    article.setLastUpdateTime(time);
    article.setPublishedTimestamp(timestamp);

    session.persist(article);

    session.getTransaction().commit();
    session.close();

    //Fetch in new session

    session = sessionFactory.openSession();
    session.beginTransaction();

    ArticleEntity fetchedArticle = session.get(ArticleEntity.class,
        article.getId());

    Assertions.assertEquals(fetchedArticle.getLastUpdateDate(), date);
    Assertions.assertEquals(fetchedArticle.getLastUpdateTime(), time);
    Assertions.assertEquals(fetchedArticle.getPublishedTimestamp(), timestamp);
  }
}
