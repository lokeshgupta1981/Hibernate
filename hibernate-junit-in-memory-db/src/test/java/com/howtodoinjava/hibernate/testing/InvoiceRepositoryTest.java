package com.howtodoinjava.hibernate.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvoiceRepositoryTest {

  private static EntityManagerFactory emf;

  private EntityManager em;
  private InvoiceRepository repository;

  @BeforeAll
  static void createFactory() {
    emf = Database.inMemory(true);                   // create tables + run import.sql
  }

  @BeforeEach
  void resetData() {
    emf.getSchemaManager().truncate();               // empty tables + run import.sql again
    em = emf.createEntityManager();
    em.getTransaction().begin();
    repository = new InvoiceRepository(em);
  }

  @AfterEach
  void closeEntityManager() {
    if (em.getTransaction().isActive()) {
      em.getTransaction().rollback();
    }
    em.close();
  }

  @AfterAll
  static void closeFactory() {
    emf.close();                                     // drop tables
  }

  @Test
  void startsWithRowsFromImportSql() {
    List<Long> ids = em.createQuery("select id from Invoice order by id", Long.class).getResultList();
    assertEquals(List.of(1L, 2L, 3L), ids);
  }

  @Test
  void findsUnpaidInvoices() {
    List<Invoice> unpaid = repository.findUnpaid();
    assertEquals("[Lokesh 250.00 (open), Alex 120.00 (open)]", unpaid.toString());
  }

  @Test
  void savesNewInvoice() {
    Invoice invoice = repository.save(new Invoice("Maria", "90.00", LocalDate.of(2026, 9, 25)));
    assertEquals(4L, invoice.getId());
    assertTrue(repository.findById(4L).isPresent());
    assertEquals(3, repository.findUnpaid().size());
  }

  @Test
  void sumsOutstandingAmountPerClient() {
    assertEquals(new BigDecimal("250.00"), repository.outstandingFor("Lokesh"));
    assertEquals(new BigDecimal("120.00"), repository.outstandingFor("Alex"));
    assertEquals(0, BigDecimal.ZERO.compareTo(repository.outstandingFor("Maria")));
  }

  @Test
  void marksAllInvoicesOfClientPaid() {
    int updated = repository.markAllPaid("Lokesh");
    assertEquals(2, updated);
    assertEquals(0, BigDecimal.ZERO.compareTo(repository.outstandingFor("Lokesh")));
  }

  @Test
  void committedChangesDoNotLeakIntoOtherTests() {
    em.createQuery("delete from Invoice").executeUpdate();
    em.getTransaction().commit();                    // really deleted
    assertEquals(0L, em.createQuery("select count(*) from Invoice", Long.class).getSingleResult());
    // the next test still starts with 3 rows, because @BeforeEach truncates and reloads
  }
}
