package com.howtodoinjava.hibernate.jndi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManagerFactory;
import javax.naming.Context;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;
import org.junit.jupiter.api.Test;

/** No JNDI at all: Hibernate gets the DataSource object. */
class DirectDataSourceTest {

  @Test
  void jakartaNonJtaDataSourceAcceptsTheObject() {
    try (EntityManagerFactory emf = Database.fromDataSource(Database.h2("direct"), false)) {
      emf.runInTransaction(em -> em.persist(new City("Lisbon", "Portugal", 545000)));
      assertEquals(1L, count(emf));
    }
  }

  @Test
  void hibernateConnectionDatasourceAcceptsTheObject() {
    try (EntityManagerFactory emf = new HibernatePersistenceConfiguration("cities-direct")
        .managedClasses(City.class)
        .property(JdbcSettings.DATASOURCE, Database.h2("direct2"))
        .schemaToolingAction(Action.CREATE_DROP)
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(new City("Porto", "Portugal", 232000)));
      assertEquals(1L, count(emf));
    }
  }

  @Test
  void worksWithoutAnyJndiProvider() {
    String previous = System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
    Thread thread = Thread.currentThread();
    ClassLoader original = thread.getContextClassLoader();
    thread.setContextClassLoader(new ClassLoader(original) {
      @Override
      public java.net.URL getResource(String name) {
        return "jndi.properties".equals(name) ? null : super.getResource(name);
      }

      @Override
      public java.util.Enumeration<java.net.URL> getResources(String name) throws java.io.IOException {
        return "jndi.properties".equals(name)
            ? java.util.Collections.emptyEnumeration() : super.getResources(name);
      }
    });
    try (EntityManagerFactory emf = Database.fromDataSource(Database.h2("direct3"), false)) {
      emf.runInTransaction(em -> em.persist(new City("Madrid", "Spain", 3300000)));
      assertEquals(1L, count(emf));
    } finally {
      thread.setContextClassLoader(original);
      if (previous != null) {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, previous);
      }
    }
  }

  private static long count(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from City", Long.class).getSingleResult());
  }
}
