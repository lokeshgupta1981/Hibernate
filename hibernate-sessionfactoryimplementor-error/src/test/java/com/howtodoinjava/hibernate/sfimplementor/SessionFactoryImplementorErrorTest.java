package com.howtodoinjava.hibernate.sfimplementor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

class SessionFactoryImplementorErrorTest {

  private static EntityManagerFactory emf;

  @BeforeAll
  static void start() {
    emf = Database.create(false);
    Database.saveSampleData(emf);
  }

  @AfterAll
  static void stop() {
    emf.close();
  }

  // 1. Where the interface lives in Hibernate 7

  @Test
  void oldPackageIsGone() {
    ClassNotFoundException e = assertThrows(ClassNotFoundException.class,
        () -> Class.forName("org.hibernate.engine.SessionFactoryImplementor"));
    assertEquals("org.hibernate.engine.SessionFactoryImplementor", e.getMessage());
  }

  @Test
  void interfaceLivesInSpiPackage() throws ClassNotFoundException {
    Class<?> type = Class.forName("org.hibernate.engine.spi.SessionFactoryImplementor");
    assertTrue(type.isInterface());
    assertTrue(SessionFactory.class.isAssignableFrom(type));
  }

  // 2. The error: Spring's orm.hibernate3 code on Hibernate 7

  @Test
  void hibernate3TransactionManagerFails() {
    HibernateTransactionManager txManager = new HibernateTransactionManager();
    txManager.setSessionFactory(emf.unwrap(SessionFactory.class));

    NoClassDefFoundError e = assertThrows(NoClassDefFoundError.class, txManager::afterPropertiesSet);
    assertEquals("org/hibernate/engine/SessionFactoryImplementor", e.getMessage());
    assertInstanceOf(ClassNotFoundException.class, e.getCause());
    assertEquals("org.hibernate.engine.SessionFactoryImplementor", e.getCause().getMessage());

    StackTraceElement[] frames = e.getStackTrace();
    assertEquals("org.springframework.orm.hibernate3.SessionFactoryUtils", frames[0].getClassName());
    assertEquals("getDataSource", frames[0].getMethodName());
    assertEquals("org.springframework.orm.hibernate3.HibernateTransactionManager", frames[1].getClassName());
    assertEquals("afterPropertiesSet", frames[1].getMethodName());
  }

  @Test
  void sessionFactoryUtilsFailsDirectly() {
    SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
    NoClassDefFoundError e = assertThrows(NoClassDefFoundError.class,
        () -> SessionFactoryUtils.getDataSource(sessionFactory));
    assertEquals("org/hibernate/engine/SessionFactoryImplementor", e.getMessage());
  }

  // 3. Finding the jar

  @Test
  void codeSourceNamesTheJar() {
    String location = SessionFactoryUtils.class.getProtectionDomain().getCodeSource().getLocation().toString();
    assertTrue(location.endsWith("spring-orm-3.2.18.RELEASE.jar"), location);
  }

  @Test
  void scannerFindsEveryClassThatUsesTheOldName() throws Exception {
    List<String> hits = ClasspathScanner.findReferences("org/hibernate/engine/SessionFactoryImplementor");
    assertEquals(List.of(
        "spring-orm-3.2.18.RELEASE.jar: org.springframework.orm.hibernate3.LocalSessionFactoryBean",
        "spring-orm-3.2.18.RELEASE.jar: org.springframework.orm.hibernate3.SessionFactoryUtils",
        "spring-orm-3.2.18.RELEASE.jar: org.springframework.orm.hibernate3.SpringSessionContext"),
        hits.stream().sorted().toList());
  }

  // 4. The Hibernate 7 way: unwrap the SPI interface

  @Test
  void unwrapReturnsTheSameFactory() {
    SessionFactoryImplementor sfi = emf.unwrap(SessionFactoryImplementor.class);
    assertSame(emf.unwrap(SessionFactory.class), sfi);
    SessionFactoryImplementor fromSession = emf.callInTransaction(em -> em.unwrap(SessionImplementor.class).getFactory());
    assertSame(sfi, fromSession);
  }

  @Test
  void spiGivesDialectAndEntityDescriptor() {
    SessionFactoryImplementor sfi = emf.unwrap(SessionFactoryImplementor.class);
    assertInstanceOf(H2Dialect.class, sfi.getJdbcServices().getDialect());
    assertEquals("com.howtodoinjava.hibernate.sfimplementor.Fixture",
        sfi.getMappingMetamodel().getEntityDescriptor(Fixture.class).getEntityName());
  }

  @Test
  void fixturesAreSavedAndRead() {
    List<String> games = emf.callInTransaction(em -> em.createQuery(
            "select f from Fixture f order by f.kickoff", Fixture.class)
        .getResultList().stream().map(f -> f.getHomeTeam() + " vs " + f.getAwayTeam()).toList());
    assertEquals(List.of("Rovers vs United", "City vs Athletic"), games);
  }
}
