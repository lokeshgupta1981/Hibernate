package com.howtodoinjava.hibernate.jndi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.sql.DataSource;
import org.hibernate.engine.jndi.JndiException;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Hibernate looks up an H2 DataSource bound in our own InMemoryContextFactory. */
class InMemoryJndiTest {

  private static DataSource h2;
  private static EntityManagerFactory emf;
  private Long lisbonId;

  @BeforeAll
  static void bindDataSource() throws NamingException {
    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryContextFactory.class.getName());
    h2 = Database.h2("cities");
    new InitialContext().bind(Database.JNDI_NAME, h2);
    emf = Database.fromJndi(Database.JNDI_NAME, false);
  }

  @AfterAll
  static void unbindDataSource() {
    emf.close();
    InMemoryContextFactory.clear();
    System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
  }

  @BeforeEach
  void loadCities() {
    emf.getSchemaManager().truncate();
    lisbonId = emf.callInTransaction(em -> {
      City lisbon = new City("Lisbon", "Portugal", 545000);
      em.persist(lisbon);
      em.persist(new City("Porto", "Portugal", 232000));
      em.persist(new City("Madrid", "Spain", 3300000));
      return lisbon.getId();
    });
  }

  @Test
  void lookupReturnsTheBoundDataSource() throws NamingException {
    // jndi.properties names Simple-JNDI, which also defines this name; the system property wins
    assertSame(h2, new InitialContext().lookup(Database.JNDI_NAME));
  }

  @Test
  void hibernateWritesThroughTheBoundDataSource() throws Exception {
    try (Connection connection = h2.getConnection();
         ResultSet rs = connection.createStatement().executeQuery("select count(*) from City")) {
      rs.next();
      assertEquals(3, rs.getLong(1));
    }
  }

  @Test
  void findReadsTheCity() {
    City lisbon = emf.callInTransaction(em -> em.find(City.class, lisbonId));
    assertEquals("Lisbon (Portugal, 545000)", lisbon.toString());
  }

  @Test
  void updateChangesThePopulation() {
    emf.runInTransaction(em -> em.find(City.class, lisbonId).setPopulation(548000));
    assertEquals(548000, emf.callInTransaction(em -> em.find(City.class, lisbonId)).getPopulation());
  }

  @Test
  void queryByCountry() {
    List<String> portugal = emf.callInTransaction(em -> em
        .createQuery("select c.name from City c where c.country = :country order by c.name", String.class)
        .setParameter("country", "Portugal")
        .getResultList());
    assertEquals(List.of("Lisbon", "Porto"), portugal);
  }

  @Test
  void removeDeletesTheCity() {
    emf.runInTransaction(em -> em.remove(em.find(City.class, lisbonId)));
    assertNull(emf.callInTransaction(em -> em.find(City.class, lisbonId)));
    assertEquals(2L, (long) emf.callInTransaction(em ->
        em.createQuery("select count(*) from City", Long.class).getSingleResult()));
  }

  @Test
  void unboundNameFailsAtBootstrap() {
    RuntimeException e = assertThrows(RuntimeException.class,
        () -> Database.fromJndi("java:comp/env/jdbc/towns", false));
    JndiException jndi = assertInstanceOf(JndiException.class, e.getCause());
    assertEquals("Unable to lookup JNDI name [java:comp/env/jdbc/towns]", jndi.getMessage());
    assertInstanceOf(NameNotFoundException.class, jndi.getCause());
  }

  @Test
  void ldapNamesAreRejected() {
    RuntimeException e = assertThrows(RuntimeException.class,
        () -> Database.fromJndi("ldap://localhost:389/cn=cities", false));
    assertEquals("JNDI lookups for scheme 'ldap' are not allowed", e.getCause().getMessage());
  }

  @Test
  void hibernateJndiClassWinsOverSystemPropertyAndJndiProperties() {
    // system property and jndi.properties both point to Simple-JNDI, which has no "cities" database
    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.osjava.sj.SimpleContextFactory");
    try (EntityManagerFactory other = new HibernatePersistenceConfiguration("cities-jndi-class")
        .managedClasses(City.class)
        .nonJtaDataSource(Database.JNDI_NAME)
        .property("hibernate.jndi.class", InMemoryContextFactory.class.getName())
        .createEntityManagerFactory()) {
      assertEquals(3L, (long) other.callInTransaction(em ->
          em.createQuery("select count(*) from City", Long.class).getSingleResult()));
    } finally {
      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryContextFactory.class.getName());
    }
  }

  @Test
  void withoutProviderTheLookupFails() {
    System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
    Thread thread = Thread.currentThread();
    ClassLoader original = thread.getContextClassLoader();
    thread.setContextClassLoader(new ClassLoader(null) { });   // hides jndi.properties
    try {
      NoInitialContextException e = assertThrows(NoInitialContextException.class,
          () -> new InitialContext().lookup(Database.JNDI_NAME));
      assertEquals("Need to specify class name in environment or system property, "
          + "or in an application resource file: java.naming.factory.initial", e.getMessage());
    } finally {
      thread.setContextClassLoader(original);
      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryContextFactory.class.getName());
    }
  }
}
