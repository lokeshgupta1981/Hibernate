package com.howtodoinjava.hibernate.jndi;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class JndiDemo {

  public static void main(String[] args) throws Exception {

    step("1. Look up the DataSource outside a server, with no JNDI provider");
    try {
      new InitialContext().lookup(Database.JNDI_NAME);
    } catch (NamingException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
    }

    step("2. Register the in-memory provider and bind an H2 DataSource");
    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryContextFactory.class.getName());
    DataSource h2 = Database.h2("cities");
    new InitialContext().bind(Database.JNDI_NAME, h2);
    Object found = new InitialContext().lookup(Database.JNDI_NAME);
    System.out.println("lookup returns the bound object: " + (found == h2));

    step("3. Bootstrap Hibernate with the JNDI name");
    try (EntityManagerFactory emf = Database.fromJndi(Database.JNDI_NAME, true)) {
      crud(emf);
    }

    step("4. JNDI name that is not bound");
    try {
      Database.fromJndi("java:comp/env/jdbc/towns", false).close();
    } catch (RuntimeException e) {
      printCauses(e);
    }

    step("5. JNDI name with the ldap scheme");
    try {
      Database.fromJndi("ldap://localhost:389/cn=cities", false).close();
    } catch (RuntimeException e) {
      printCauses(e);
    }

    step("6. Pass the DataSource object directly, no JNDI");
    try (EntityManagerFactory emf = Database.fromDataSource(Database.h2("direct"), true)) {
      emf.runInTransaction(em -> em.persist(new City("Lisbon", "Portugal", 545000)));
      System.out.println("cities = " + count(emf));
    }
  }

  private static void crud(EntityManagerFactory emf) {
    step("3.1 Create");
    Long lisbonId = emf.callInTransaction(em -> {
      City lisbon = new City("Lisbon", "Portugal", 545000);
      em.persist(lisbon);
      em.persist(new City("Porto", "Portugal", 232000));
      em.persist(new City("Madrid", "Spain", 3300000));
      return lisbon.getId();
    });

    step("3.2 Read");
    City lisbon = emf.callInTransaction(em -> em.find(City.class, lisbonId));
    System.out.println("find = " + lisbon);

    step("3.3 Update");
    emf.runInTransaction(em -> em.find(City.class, lisbonId).setPopulation(548000));

    step("3.4 Query");
    List<String> portugal = emf.callInTransaction(em -> em
        .createQuery("select c.name from City c where c.country = :country order by c.name", String.class)
        .setParameter("country", "Portugal")
        .getResultList());
    System.out.println("portugal = " + portugal);

    step("3.5 Delete");
    emf.runInTransaction(em -> em.remove(em.find(City.class, lisbonId)));
    System.out.println("cities = " + count(emf));
  }

  private static long count(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from City", Long.class).getSingleResult());
  }

  private static void printCauses(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      System.out.println(t.getClass().getName() + ": " + t.getMessage());
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
