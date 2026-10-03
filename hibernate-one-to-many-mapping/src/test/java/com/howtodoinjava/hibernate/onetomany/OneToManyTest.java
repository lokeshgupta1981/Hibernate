package com.howtodoinjava.hibernate.onetomany;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.howtodoinjava.hibernate.onetomany.bidirectional.Account;
import com.howtodoinjava.hibernate.onetomany.bidirectional.Payment;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OneToManyTest {

  private static List<String> sqlStartingWith(String prefix) {
    return SqlLog.statements().stream().filter(s -> s.startsWith(prefix)).toList();
  }

  @Test
  void collectionsAreLazyAndManyToOneIsEagerByDefault() throws Exception {
    assertEquals(FetchType.LAZY, OneToMany.class.getMethod("fetch").getDefaultValue());
    assertEquals(FetchType.EAGER, ManyToOne.class.getMethod("fetch").getDefaultValue());
  }

  @Nested
  class Bidirectional {

    private Long saveLokesh(EntityManagerFactory emf) {
      return emf.callInTransaction(em -> {
        Account account = new Account("Lokesh");
        account.addPayment(new Payment("Rent", "1200.00"));
        account.addPayment(new Payment("Groceries", "85.50"));
        account.addPayment(new Payment("Coffee", "4.00"));
        em.persist(account);
        return account.getId();
      });
    }

    private EntityManagerFactory emf() {
      return Database.create("bidi" + System.nanoTime(), false, Account.class, Payment.class);
    }

    @Test
    void saveRunsOneInsertPerRow() {
      try (EntityManagerFactory emf = emf()) {
        SqlLog.clear();
        saveLokesh(emf);
        assertEquals(4, SqlLog.statements().size());
        assertEquals(1, sqlStartingWith("insert into Account").size());
        assertEquals(3, sqlStartingWith("insert into Payment (account_id,").size());
        assertEquals(1, Database.count(emf, "Account"));
        assertEquals(3, Database.count(emf, "Payment"));
      }
    }

    @Test
    void paymentsAreLoadedOnFirstAccessInAmountOrder() {
      try (EntityManagerFactory emf = emf()) {
        Long id = saveLokesh(emf);
        emf.runInTransaction(em -> {
          SqlLog.clear();
          Account account = em.find(Account.class, id);
          assertFalse(Hibernate.isInitialized(account.getPayments()));
          assertEquals(1, SqlLog.statements().size());
          assertEquals("[Rent 1200.00, Groceries 85.50, Coffee 4.00]",
              account.getPayments().toString());
          assertEquals(2, SqlLog.statements().size());
          assertTrue(SqlLog.statements().get(1).endsWith("order by p1_0.amount desc"));
        });
      }
    }

    @Test
    void removingAPaymentRunsOneDelete() {
      try (EntityManagerFactory emf = emf()) {
        Long id = saveLokesh(emf);
        SqlLog.clear();
        emf.runInTransaction(em -> {
          Account account = em.find(Account.class, id);
          account.removePayment(account.findPayment("Coffee"));
        });
        assertEquals(List.of("delete from Payment where id=?"), sqlStartingWith("delete"));
        assertEquals(0, SqlLog.count("update"));
        assertEquals(2, Database.count(emf, "Payment"));
      }
    }

    @Test
    void addingOnlyToTheInverseSideLeavesForeignKeyNull() {
      try (EntityManagerFactory emf = emf()) {
        Long id = saveLokesh(emf);
        emf.runInTransaction(em ->
            em.find(Account.class, id).getPayments().add(new Payment("Books", "30.00")));
        Object accountId = emf.callInTransaction(em -> em
            .createNativeQuery("select account_id from Payment where description = 'Books'")
            .getSingleResult());
        assertNull(accountId);
        List<String> descriptions = emf.callInTransaction(em -> em.find(Account.class, id)
            .getPayments().stream().map(Payment::getDescription).toList());
        assertEquals(List.of("Rent", "Groceries", "Coffee"), descriptions);
      }
    }

    private void saveThreeAccounts(EntityManagerFactory emf) {
      saveLokesh(emf);
      emf.runInTransaction(em -> {
        Account alex = new Account("Alex");
        alex.addPayment(new Payment("Gym", "40.00"));
        alex.addPayment(new Payment("Taxi", "18.00"));
        em.persist(alex);
        Account maria = new Account("Maria");
        maria.addPayment(new Payment("Phone", "25.00"));
        em.persist(maria);
      });
    }

    @Test
    void readingEachCollectionRunsNPlusOneSelects() {
      try (EntityManagerFactory emf = emf()) {
        saveThreeAccounts(emf);
        SqlLog.clear();
        emf.runInTransaction(em -> em.createQuery("from Account", Account.class).getResultList()
            .forEach(a -> a.getPayments().size()));
        assertEquals(4, SqlLog.count("select"));
      }
    }

    @Test
    void joinFetchRunsOneSelect() {
      try (EntityManagerFactory emf = emf()) {
        saveThreeAccounts(emf);
        SqlLog.clear();
        List<Integer> sizes = emf.callInTransaction(em -> em
            .createQuery("from Account a left join fetch a.payments", Account.class)
            .getResultList().stream().map(a -> a.getPayments().size()).toList());
        assertEquals(List.of(3, 2, 1), sizes);
        assertEquals(1, SqlLog.count("select"));
        assertTrue(SqlLog.statements().get(0).contains("left join Payment"));
      }
    }

    @Test
    void batchFetchSizeRunsTwoSelects() {
      try (EntityManagerFactory emf = Database.create("batch" + System.nanoTime(), false,
          Map.of("hibernate.default_batch_fetch_size", 10), Account.class, Payment.class)) {
        saveThreeAccounts(emf);
        SqlLog.clear();
        emf.runInTransaction(em -> em.createQuery("from Account", Account.class).getResultList()
            .forEach(a -> a.getPayments().size()));
        assertEquals(2, SqlLog.count("select"));
        assertTrue(SqlLog.statements().get(1).contains("account_id in (?,?,?,?,?,?,?,?,?,?)"));
      }
    }

    @Test
    void readingPaymentsAfterTheTransactionFails() {
      try (EntityManagerFactory emf = emf()) {
        Long id = saveLokesh(emf);
        Account detached = emf.callInTransaction(em -> em.find(Account.class, id));
        LazyInitializationException e = assertThrows(LazyInitializationException.class,
            () -> detached.getPayments().size());
        assertTrue(e.getMessage().startsWith("Cannot lazily initialize collection of role"));
      }
    }
  }

  @Nested
  class UnidirectionalJoinColumn {

    @Test
    void saveRunsExtraUpdatesAndRemoveRunsUpdateThenDelete() {
      try (EntityManagerFactory emf = Database.create("jc" + System.nanoTime(), false,
          com.howtodoinjava.hibernate.onetomany.joincolumn.Account.class,
          com.howtodoinjava.hibernate.onetomany.joincolumn.Payment.class)) {
        SqlLog.clear();
        Long id = emf.callInTransaction(em -> {
          var account = new com.howtodoinjava.hibernate.onetomany.joincolumn.Account("Lokesh");
          account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.joincolumn.Payment("Rent", "1200.00"));
          account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.joincolumn.Payment("Groceries", "85.50"));
          account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.joincolumn.Payment("Coffee", "4.00"));
          em.persist(account);
          return account.getId();
        });
        assertEquals(7, SqlLog.statements().size());
        assertEquals(3, sqlStartingWith("insert into Payment (amount,description,id)").size());
        assertEquals(3, sqlStartingWith("update Payment set account_id=? where id=?").size());

        SqlLog.clear();
        emf.runInTransaction(em -> {
          var account = em.find(com.howtodoinjava.hibernate.onetomany.joincolumn.Account.class, id);
          account.getPayments().remove(account.findPayment("Coffee"));
        });
        assertEquals(List.of("update Payment set account_id=null where account_id=? and id=?"),
            sqlStartingWith("update"));
        assertEquals(List.of("delete from Payment where id=?"), sqlStartingWith("delete"));
        assertEquals(2, Database.count(emf, "Payment"));
      }
    }
  }

  @Nested
  class UnidirectionalJoinTable {

    @Test
    void saveFillsTheJoinTableAndRemoveRewritesIt() {
      try (EntityManagerFactory emf = Database.create("jt" + System.nanoTime(), false,
          com.howtodoinjava.hibernate.onetomany.jointable.Account.class,
          com.howtodoinjava.hibernate.onetomany.jointable.Payment.class)) {
        SqlLog.clear();
        Long id = emf.callInTransaction(em -> {
          var account = new com.howtodoinjava.hibernate.onetomany.jointable.Account("Lokesh");
          account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.jointable.Payment("Rent", "1200.00"));
          account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.jointable.Payment("Groceries", "85.50"));
          account.getPayments().add(new com.howtodoinjava.hibernate.onetomany.jointable.Payment("Coffee", "4.00"));
          em.persist(account);
          return account.getId();
        });
        assertEquals(7, SqlLog.count("insert"));
        assertEquals(3, sqlStartingWith("insert into Account_Payment (Account_id,payments_id)").size());
        Number links = emf.callInTransaction(em -> (Number) em
            .createNativeQuery("select count(*) from Account_Payment").getSingleResult());
        assertEquals(3, links.intValue());

        SqlLog.clear();
        emf.runInTransaction(em -> {
          var account = em.find(com.howtodoinjava.hibernate.onetomany.jointable.Account.class, id);
          account.getPayments().remove(account.findPayment("Coffee"));
        });
        assertEquals(List.of(
            "delete from Account_Payment where Account_id=?",
            "insert into Account_Payment (Account_id,payments_id) values (?,?)",
            "insert into Account_Payment (Account_id,payments_id) values (?,?)",
            "delete from Payment where id=?"), SqlLog.statements().subList(2, 6));
        assertEquals(2, Database.count(emf, "Payment"));
      }
    }
  }
}
