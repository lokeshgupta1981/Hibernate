package com.howtodoinjava.hibernate.onetomany;

import com.howtodoinjava.hibernate.onetomany.bidirectional.Account;
import com.howtodoinjava.hibernate.onetomany.bidirectional.Payment;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;

public class OneToManyDemo {

  public static void main(String[] args) {
    bidirectional();
    unidirectionalJoinColumn();
    unidirectionalJoinTable();
    batchFetch();
  }

  // Recommended mapping: @OneToMany(mappedBy) + @ManyToOne
  static void bidirectional() {
    try (EntityManagerFactory emf = Database.create("bidirectional", true,
        Account.class, Payment.class)) {

      step("1. Bidirectional: save an account with three payments");
      Long accountId = emf.callInTransaction(em -> {
        Account account = new Account("Lokesh");
        account.addPayment(new Payment("Rent", "1200.00"));
        account.addPayment(new Payment("Groceries", "85.50"));
        account.addPayment(new Payment("Coffee", "4.00"));
        em.persist(account);
        return account.getId();
      });
      statements();

      step("2. Bidirectional: load the account, then its payments (LAZY, @OrderBy amount desc)");
      emf.runInTransaction(em -> {
        Account account = em.find(Account.class, accountId);
        System.out.println("-- account loaded, now reading payments");
        System.out.println(account.getPayments());
      });
      statements();

      step("3. Bidirectional: remove the Coffee payment");
      emf.runInTransaction(em -> {
        Account account = em.find(Account.class, accountId);
        account.removePayment(account.findPayment("Coffee"));
      });
      statements();

      step("4. Owning side not set: add to the collection only");
      emf.runInTransaction(em -> {
        Account account = em.find(Account.class, accountId);
        account.getPayments().add(new Payment("Books", "30.00"));
      });
      System.out.println("account_id of Books = " + emf.callInTransaction(em -> em
          .createNativeQuery("select account_id from Payment where description = 'Books'")
          .getSingleResult()));

      step("5. Save two more accounts");
      emf.runInTransaction(em -> {
        Account alex = new Account("Alex");
        alex.addPayment(new Payment("Gym", "40.00"));
        alex.addPayment(new Payment("Taxi", "18.00"));
        em.persist(alex);
        Account maria = new Account("Maria");
        maria.addPayment(new Payment("Phone", "25.00"));
        em.persist(maria);
      });

      step("6. N+1: load all accounts, then read each account's payments");
      emf.runInTransaction(em -> {
        List<Account> accounts = em.createQuery("from Account", Account.class).getResultList();
        accounts.forEach(a -> System.out.println(a.getOwner() + " " + a.getPayments().size()));
      });
      statements();

      step("7. join fetch: accounts and payments in one query");
      emf.runInTransaction(em -> {
        List<Account> accounts = em.createQuery(
            "from Account a left join fetch a.payments", Account.class).getResultList();
        accounts.forEach(a -> System.out.println(a.getOwner() + " " + a.getPayments().size()));
      });
      statements();

      step("8. Read payments after the transaction ended");
      Account detached = emf.callInTransaction(em -> em.find(Account.class, accountId));
      try {
        detached.getPayments().size();
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }
  }

  static void unidirectionalJoinColumn() {
    try (EntityManagerFactory emf = Database.create("joincolumn", true,
        com.howtodoinjava.hibernate.onetomany.joincolumn.Account.class,
        com.howtodoinjava.hibernate.onetomany.joincolumn.Payment.class)) {

      step("9. Unidirectional @JoinColumn: save an account with three payments");
      Long accountId = emf.callInTransaction(em -> {
        var account = new com.howtodoinjava.hibernate.onetomany.joincolumn.Account("Lokesh");
        account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.joincolumn.Payment("Rent", "1200.00"));
        account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.joincolumn.Payment("Groceries", "85.50"));
        account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.joincolumn.Payment("Coffee", "4.00"));
        em.persist(account);
        return account.getId();
      });
      statements();

      step("10. Unidirectional @JoinColumn: remove the Coffee payment");
      emf.runInTransaction(em -> {
        var account = em.find(com.howtodoinjava.hibernate.onetomany.joincolumn.Account.class, accountId);
        account.getPayments().remove(account.findPayment("Coffee"));
      });
      statements();
    }
  }

  static void unidirectionalJoinTable() {
    step("11. Unidirectional join table: schema");
    try (EntityManagerFactory emf = Database.create("jointable", true,
        com.howtodoinjava.hibernate.onetomany.jointable.Account.class,
        com.howtodoinjava.hibernate.onetomany.jointable.Payment.class)) {

      step("12. Unidirectional join table: save an account with three payments");
      Long accountId = emf.callInTransaction(em -> {
        var account = new com.howtodoinjava.hibernate.onetomany.jointable.Account("Lokesh");
        account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.jointable.Payment("Rent", "1200.00"));
        account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.jointable.Payment("Groceries", "85.50"));
        account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.jointable.Payment("Coffee", "4.00"));
        em.persist(account);
        return account.getId();
      });
      statements();

      step("13. Unidirectional join table: remove the Coffee payment");
      emf.runInTransaction(em -> {
        var account = em.find(com.howtodoinjava.hibernate.onetomany.jointable.Account.class, accountId);
        account.getPayments().remove(account.findPayment("Coffee"));
      });
      statements();
    }
  }

  static void batchFetch() {
    try (EntityManagerFactory emf = Database.create("batchfetch", false,
        Map.of("hibernate.default_batch_fetch_size", 10), Account.class, Payment.class)) {
      emf.runInTransaction(em -> {
        for (String owner : List.of("Lokesh", "Alex", "Maria")) {
          Account account = new Account(owner);
          account.addPayment(new Payment("Rent", "1200.00"));
          em.persist(account);
        }
      });
      step("14. default_batch_fetch_size = 10: load all accounts, then their payments");
      emf.runInTransaction(em -> {
        List<Account> accounts = em.createQuery("from Account", Account.class).getResultList();
        accounts.forEach(a -> a.getPayments().size());
      });
      SqlLog.statements().forEach(s -> System.out.println("Hibernate: " + s));
      statements();
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
    SqlLog.clear();
  }

  private static void statements() {
    System.out.println("-- statements: " + SqlLog.statements().size()
        + " (select " + SqlLog.count("select") + ", insert " + SqlLog.count("insert")
        + ", update " + SqlLog.count("update") + ", delete " + SqlLog.count("delete") + ")");
  }
}
