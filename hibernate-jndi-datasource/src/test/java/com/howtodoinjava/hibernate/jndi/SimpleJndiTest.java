package com.howtodoinjava.hibernate.jndi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManagerFactory;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Simple-JNDI reads src/test/resources/jndi.properties and creates the DataSource
 * declared in src/test/resources/jndi/jdbc.properties.
 */
class SimpleJndiTest {

  private static EntityManagerFactory emf;

  @BeforeAll
  static void bootstrap() {
    emf = Database.fromJndi(Database.JNDI_NAME, false);
  }

  @AfterAll
  static void close() {
    emf.close();
  }

  @Test
  void theDataSourceComesFromThePropertiesFile() throws NamingException {
    DataSource dataSource = (DataSource) new InitialContext().lookup(Database.JNDI_NAME);
    assertEquals("org.h2.Driver::::jdbc:h2:mem:simple;DB_CLOSE_DELAY=-1::::sa", dataSource.toString());
  }

  @Test
  void hibernateRunsCrudOnIt() {
    Long id = emf.callInTransaction(em -> {
      City madrid = new City("Madrid", "Spain", 3300000);
      em.persist(madrid);
      return madrid.getId();
    });
    emf.runInTransaction(em -> em.find(City.class, id).setPopulation(3400000));
    assertEquals(3400000, emf.callInTransaction(em -> em.find(City.class, id)).getPopulation());
    emf.runInTransaction(em -> em.remove(em.find(City.class, id)));
    assertEquals(0L, (long) emf.callInTransaction(em ->
        em.createQuery("select count(*) from City", Long.class).getSingleResult()));
  }

  @Test
  void ignoreCloseKeepsBindingsAfterHibernateLookup() throws NamingException {
    DataSource extra = Database.h2("extra");
    new InitialContext().bind("java:comp/env/jdbc/extra", extra);
    Database.fromJndi("java:comp/env/jdbc/extra", false).close();   // Hibernate closes its InitialContext
    assertSame(extra, new InitialContext().lookup("java:comp/env/jdbc/extra"));
  }

  @Test
  void withoutIgnoreCloseHibernateWipesProgrammaticBindings() throws NamingException {
    new InitialContext().bind("java:comp/env/jdbc/temp", Database.h2("temp"));
    new HibernatePersistenceConfiguration("cities-close")
        .managedClasses(City.class)
        .nonJtaDataSource("java:comp/env/jdbc/temp")
        .property("hibernate.jndi.org.osjava.sj.jndi.ignoreClose", "false")
        .createEntityManagerFactory()
        .close();
    assertThrows(NameNotFoundException.class,
        () -> new InitialContext().lookup("java:comp/env/jdbc/temp"));
  }
}
