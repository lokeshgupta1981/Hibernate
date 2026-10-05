package com.howtodoinjava.hibernate.springconfig;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.hibernate.LocalSessionFactoryBean;

/**
 * Tests every result shown in the article. The XML tests start their own plain Spring contexts;
 * the Spring Boot tests use the context started by @SpringBootTest.
 */
@SpringBootTest
class ConfigurationClassTest {

  private static final LocalDateTime EVENING = LocalDateTime.of(2026, 10, 9, 18, 30);

  // ---------- 1. The error ----------

  @Test
  void configurationClassPropertyFailsOnSpring7LocalSessionFactoryBean() {
    BeanCreationException e = assertThrows(BeanCreationException.class,
        () -> new GenericXmlApplicationContext("legacy/session-factory-broken.xml"));

    assertEquals("Error creating bean with name 'sessionFactory' defined in class path resource "
        + "[legacy/session-factory-broken.xml]: Invalid property 'configurationClass' of bean class "
        + "[org.springframework.orm.jpa.hibernate.LocalSessionFactoryBean]: Bean property 'configurationClass' "
        + "is not writable or has an invalid setter method. Does the parameter type of the setter match "
        + "the return type of the getter?", e.getMessage());

    NotWritablePropertyException cause = assertInstanceOf(NotWritablePropertyException.class,
        e.getMostSpecificCause());
    assertEquals("Invalid property 'configurationClass' of bean class "
        + "[org.springframework.orm.jpa.hibernate.LocalSessionFactoryBean]: Bean property 'configurationClass' "
        + "is not writable or has an invalid setter method. Does the parameter type of the setter match "
        + "the return type of the getter?", cause.getMessage());
    assertEquals("configurationClass", cause.getPropertyName());
    assertEquals(LocalSessionFactoryBean.class, cause.getBeanClass());
  }

  @Test
  void localSessionFactoryBeanHasNoConfigurationClassProperty() {
    BeanWrapperImpl wrapper = new BeanWrapperImpl(LocalSessionFactoryBean.class);

    assertFalse(wrapper.isWritableProperty("configurationClass"));
    assertTrue(wrapper.isWritableProperty("packagesToScan"));
    assertTrue(wrapper.isWritableProperty("annotatedClasses"));
    assertTrue(wrapper.isWritableProperty("hibernateProperties"));
    assertTrue(wrapper.isWritableProperty("dataSource"));
  }

  @Test
  void hibernate5PackageNoLongerExistsInSpring7() {
    CannotLoadBeanClassException e = assertThrows(CannotLoadBeanClassException.class,
        () -> new GenericXmlApplicationContext("legacy/session-factory-hibernate5.xml"));

    assertEquals("Cannot find class [org.springframework.orm.hibernate5.LocalSessionFactoryBean] for bean "
        + "with name 'sessionFactory' defined in class path resource [legacy/session-factory-hibernate5.xml]",
        e.getMessage());
    assertInstanceOf(ClassNotFoundException.class, e.getCause());
  }

  // ---------- 2. The fix ----------

  @Test
  void packagesToScanWithoutConfigurationClassStarts() {
    try (var context = new GenericXmlApplicationContext("legacy/session-factory-packages.xml")) {
      assertSavesAndReadsShowtimes(context.getBean(SessionFactory.class));
    }
  }

  @Test
  void annotatedClassesWithoutConfigurationClassStarts() {
    try (var context = new GenericXmlApplicationContext("legacy/session-factory-annotated-classes.xml")) {
      assertSavesAndReadsShowtimes(context.getBean(SessionFactory.class));
    }
  }

  private static void assertSavesAndReadsShowtimes(SessionFactory sessionFactory) {
    sessionFactory.inTransaction(session -> {
      session.persist(new Showtime("Dune", 1, EVENING));
      session.persist(new Showtime("Dune", 2, EVENING.plusMinutes(150)));
    });
    List<Showtime> shows = sessionFactory.fromTransaction(session ->
        session.createSelectionQuery("from Showtime order by startsAt", Showtime.class).getResultList());

    assertEquals("[Dune (screen 1, 2026-10-09T18:30), Dune (screen 2, 2026-10-09T21:00)]", shows.toString());
  }

  @Test
  void plainHibernateConfigurationReadsAnnotationsWithoutAnnotationConfiguration() {
    try (SessionFactory sessionFactory = new Configuration()
        .addAnnotatedClass(Showtime.class)
        .setProperty("hibernate.connection.url", "jdbc:h2:mem:plain;DB_CLOSE_DELAY=-1")
        .setProperty("hibernate.connection.username", "sa")
        .setProperty("hibernate.hbm2ddl.auto", "create-drop")
        .buildSessionFactory()) {
      assertSavesAndReadsShowtimes(sessionFactory);
    }
  }

  @Test
  void annotationConfigurationClassIsGoneFromHibernate7() {
    assertThrows(ClassNotFoundException.class, () -> Class.forName("org.hibernate.cfg.AnnotationConfiguration"));
  }

  // ---------- 3. Spring Boot 4.1.1 ----------

  @Autowired
  ApplicationContext context;

  @Autowired
  ShowtimeRepository repository;

  @Autowired
  EntityManagerFactory entityManagerFactory;

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  ShowtimeDao dao;

  @Test
  void bootCreatesNoSessionFactoryBeanButTheEntityManagerFactoryIsOne() {
    assertArrayEquals(new String[] {"entityManagerFactory"},
        context.getBeanNamesForType(SessionFactory.class));
    assertArrayEquals(new String[0], context.getBeanNamesForType(LocalSessionFactoryBean.class));
    assertTrue(entityManagerFactory instanceof SessionFactory);
    assertInstanceOf(JpaTransactionManager.class, context.getBean("transactionManager"));
  }

  @Test
  void repositorySavesAndFindsShowtimes() {
    repository.deleteAll();
    repository.save(new Showtime("Up", 3, LocalDateTime.of(2026, 10, 10, 14, 0)));
    repository.save(new Showtime("Coco", 3, LocalDateTime.of(2026, 10, 10, 11, 0)));

    assertEquals("[Coco (screen 3, 2026-10-10T11:00), Up (screen 3, 2026-10-10T14:00)]",
        repository.findByScreenOrderByStartsAt(3).toString());
  }

  @Test
  void unwrappedSessionFactoryWorks() {
    repository.deleteAll();
    repository.save(new Showtime("Up", 3, LocalDateTime.of(2026, 10, 10, 14, 0)));
    repository.save(new Showtime("Coco", 3, LocalDateTime.of(2026, 10, 10, 11, 0)));

    SessionFactory unwrapped = entityManagerFactory.unwrap(SessionFactory.class);
    long count = unwrapped.fromTransaction(session ->
        session.createSelectionQuery("select count(*) from Showtime", Long.class).getSingleResult());

    assertEquals(2, count);
  }

  @Test
  void daoUsesHibernateSessionFromEntityManager() {
    repository.deleteAll();
    dao.save(new Showtime("Up", 3, LocalDateTime.of(2026, 10, 10, 14, 0)));
    dao.save(new Showtime("Up", 1, LocalDateTime.of(2026, 10, 10, 19, 0)));

    assertEquals("[Up (screen 3, 2026-10-10T14:00), Up (screen 1, 2026-10-10T19:00)]",
        dao.findByMovie("Up").toString());
  }

  @Test
  void getCurrentSessionIsNotConfiguredInSpringBoot() {
    HibernateException e = assertThrows(HibernateException.class, () -> sessionFactory.getCurrentSession());

    assertEquals("No CurrentSessionContext configured", e.getMessage());
  }
}
