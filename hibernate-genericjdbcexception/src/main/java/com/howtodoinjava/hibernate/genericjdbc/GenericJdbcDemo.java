package com.howtodoinjava.hibernate.genericjdbc;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;
import org.hibernate.tool.schema.Action;

/**
 * Runs each failure from the article, prints the SQL Hibernate sent and the root SQLException,
 * then runs the fix.
 */
public class GenericJdbcDemo {

  private static final LocalDate PICKUP = LocalDate.of(2026, 10, 10);

  public static void main(String[] args) {

    step("1. SQLite, schema not created: GenericJDBCException");
    try (EntityManagerFactory emf = Database.sqlite("bakery1", Action.NONE, true, CakeOrder.class)
        .createEntityManagerFactory()) {
      attempt(emf, em -> em.createQuery("from CakeOrder", CakeOrder.class).getResultList());
    }

    step("2. H2, schema not created: the same mistake is an SQLGrammarException");
    try (EntityManagerFactory emf = Database.h2("bakery2", Action.NONE, true, CakeOrder.class)
        .createEntityManagerFactory()) {
      attempt(emf, em -> em.createQuery("from CakeOrder", CakeOrder.class).getResultList());
    }

    step("3. Fix: let Hibernate create the schema");
    try (EntityManagerFactory emf = Database.sqlite("bakery3", Action.CREATE_DROP, true, CakeOrder.class)
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(cake()));
      System.out.println("orders: " + emf.callInTransaction(em ->
          em.createQuery("from CakeOrder", CakeOrder.class).getResultList().size()));
    }

    step("4. Entity named Order with a field named value (both SQL keywords)");
    try (EntityManagerFactory emf = Database.h2("bakery4", Action.CREATE_DROP, true, Order.class)
        .createEntityManagerFactory()) {
      attempt(emf, em -> em.persist(order()));
    }

    step("5. Fix: hibernate.auto_quote_keyword = true");
    try (EntityManagerFactory emf = Database.h2("bakery5", Action.CREATE_DROP, true, Order.class)
        .property("hibernate.auto_quote_keyword", "true")
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(order()));
    }

    step("6. Fix: hibernate.globally_quoted_identifiers = true");
    try (EntityManagerFactory emf = Database.h2("bakery6", Action.CREATE_DROP, true, Order.class)
        .property("hibernate.globally_quoted_identifiers", "true")
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(order()));
    }

    step("7. Fix: rename with @Table and @Column");
    try (EntityManagerFactory emf = Database.h2("bakery7", Action.CREATE_DROP, true, CakeOrder.class)
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(cake()));
    }

    step("8. Wrong dialect: OracleDialect on H2");
    try (EntityManagerFactory emf = Database.h2("bakery8", Action.CREATE_DROP, true, CakeOrder.class)
        .property("hibernate.dialect", "org.hibernate.dialect.OracleDialect")
        .createEntityManagerFactory()) {
      attempt(emf, em -> em.persist(cake()));
    }

    step("9. MySQL function in a native query on H2");
    try (EntityManagerFactory emf = Database.h2("bakery9", Action.CREATE_DROP, true, CakeOrder.class)
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(cake()));
      attempt(emf, em -> em.createNativeQuery(
          "select date_format(pickupDate, '%d.%m') from cake_order").getResultList());
      System.out.println("format(): " + emf.callInTransaction(em -> em.createQuery(
          "select format(c.pickupDate as 'dd.MM') from CakeOrder c", String.class).getSingleResult()));
    }

    step("10. HSQLDB identity insert (fails only with the 1.8 driver)");
    try (EntityManagerFactory emf = Database.hsqldb("bakery10", Action.CREATE_DROP, true, IdentityOrder.class)
        .createEntityManagerFactory()) {
      attempt(emf, em -> em.persist(new IdentityOrder("lemon")));
    }
  }

  private static CakeOrder cake() {
    return new CakeOrder("Lokesh", "chocolate", PICKUP, new BigDecimal("40.00"));
  }

  private static Order order() {
    return new Order("Lokesh", "lemon", PICKUP, new BigDecimal("35.00"));
  }

  private static void attempt(EntityManagerFactory emf, Consumer<jakarta.persistence.EntityManager> work) {
    try {
      emf.runInTransaction(work);
      System.out.println("OK");
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
      System.out.println(RootCause.describe(e));
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title);
  }
}
