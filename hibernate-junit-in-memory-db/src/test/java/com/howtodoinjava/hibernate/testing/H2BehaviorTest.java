package com.howtodoinjava.hibernate.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks the H2 and load-script facts stated in the article. */
class H2BehaviorTest {

  private static List<Long> ids(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("select id from Invoice order by id", Long.class).getResultList());
  }

  private static List<String> clients(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("select client from Invoice order by id", String.class).getResultList());
  }

  @Test
  void inMemoryDatabaseVanishesWithLastConnectionWithoutCloseDelay() throws SQLException {
    try (Connection c = DriverManager.getConnection("jdbc:h2:mem:nodelay", "sa", "")) {
      c.createStatement().execute("create table invoice (id int)");
    }
    try (Connection c = DriverManager.getConnection("jdbc:h2:mem:nodelay", "sa", "")) {
      SQLException e = assertThrows(SQLException.class,
          () -> c.createStatement().executeQuery("select * from invoice"));
      assertTrue(e.getMessage().startsWith("Table \"INVOICE\" not found (this database is empty)"));
    }
  }

  @Test
  void closeDelayKeepsTheDatabaseUntilTheJvmStops() throws SQLException {
    try (Connection c = DriverManager.getConnection("jdbc:h2:mem:delay;DB_CLOSE_DELAY=-1", "sa", "")) {
      c.createStatement().execute("create table invoice (id int)");
    }
    try (Connection c = DriverManager.getConnection("jdbc:h2:mem:delay;DB_CLOSE_DELAY=-1", "sa", "")) {
      c.createStatement().executeQuery("select * from invoice");   // table still there
    }
  }

  @Test
  void databaseToLowerStoresLowerCaseTableNames() {
    try (EntityManagerFactory emf = Database.inMemory(false)) {
      Object name = emf.callInTransaction(em -> em.createNativeQuery(
          "select table_name from information_schema.tables where table_schema = 'public'")
          .getSingleResult());
      assertEquals("invoice", name);
    }
  }

  @Test
  void truncateRestartsIdentityOnlyInMySqlMode() {
    try (EntityManagerFactory emf = Database.inMemory(false)) {
      emf.getSchemaManager().truncate();
      assertEquals(List.of(1L, 2L, 3L), ids(emf));
    }
    try (EntityManagerFactory emf = Database.h2("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1")
        .createEntityManagerFactory()) {
      emf.getSchemaManager().truncate();
      assertEquals(List.of(4L, 5L, 6L), ids(emf));
    }
  }

  @Test
  void loadScriptPropertyRunsTogetherWithImportSql() {
    try (EntityManagerFactory emf = Database.h2("jdbc:h2:mem:both;DB_CLOSE_DELAY=-1")
        .property("jakarta.persistence.sql-load-script-source", "extra-invoices.sql")
        .createEntityManagerFactory()) {
      assertEquals(List.of("Maria", "Lokesh", "Lokesh", "Alex"), clients(emf));
    }
  }

  @Test
  void importFilesReplacesImportSql() {
    try (EntityManagerFactory emf = Database.h2("jdbc:h2:mem:files;DB_CLOSE_DELAY=-1")
        .property("hibernate.hbm2ddl.import_files", "extra-invoices.sql")
        .createEntityManagerFactory()) {
      assertEquals(List.of("Maria"), clients(emf));
    }
  }

  @Test
  void multiLineStatementIsSkippedByDefault() {
    try (EntityManagerFactory emf = Database.h2("jdbc:h2:mem:lines;DB_CLOSE_DELAY=-1")
        .property("hibernate.hbm2ddl.import_files", "multi-line-invoices.sql")
        .createEntityManagerFactory()) {
      assertEquals(List.of(), clients(emf));           // only a WARN in the log
    }
  }

  @Test
  void multiLineExtractorReadsStatementsOverSeveralLines() {
    try (EntityManagerFactory emf = Database.h2("jdbc:h2:mem:lines2;DB_CLOSE_DELAY=-1")
        .property("hibernate.hbm2ddl.import_files", "multi-line-invoices.sql")
        .property("hibernate.hbm2ddl.import_files_sql_extractor",
            "org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor")
        .createEntityManagerFactory()) {
      assertEquals(List.of("Maria"), clients(emf));
    }
  }

  @Test
  void fixedIdsInScriptBreakTheNextInsertInRegularMode() {
    try (EntityManagerFactory emf = Database.h2("jdbc:h2:mem:fixed;DB_CLOSE_DELAY=-1")
        .property("hibernate.hbm2ddl.import_files", "fixed-id-invoices.sql")
        .createEntityManagerFactory()) {
      PersistenceException e = assertThrows(PersistenceException.class, () ->
          emf.runInTransaction(em -> em.persist(new Invoice("Alex", "120.00", LocalDate.of(2026, 9, 20)))));
      assertTrue(e.getMessage().contains("Unique index or primary key violation"));
    }
    try (EntityManagerFactory emf = Database.h2(
            "jdbc:h2:mem:fixedmysql;MODE=MySQL;DB_CLOSE_DELAY=-1")
        .property("hibernate.hbm2ddl.import_files", "fixed-id-invoices.sql")
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(new Invoice("Alex", "120.00", LocalDate.of(2026, 9, 20))));
      assertEquals(List.of(1L, 2L), ids(emf));
    }
  }

  @Test
  void mySqlDateFormatFailsOnH2EvenInMySqlMode() {
    try (EntityManagerFactory emf = Database.inMemory(false)) {
      PersistenceException e = assertThrows(PersistenceException.class, () ->
          emf.runInTransaction(em -> em.createNativeQuery(
              "select date_format(issued_on, '%Y-%m') from invoice").getResultList()));
      assertTrue(e.getMessage().contains("Function \"date_format\" not found"));
    }
  }

  @Test
  void hqlExtractWorksOnEveryDatabase() {
    try (EntityManagerFactory emf = Database.inMemory(false)) {
      List<Object[]> rows = emf.callInTransaction(em -> em.createQuery(
          "select extract(month from issuedOn), sum(amount) from Invoice group by extract(month from issuedOn)",
          Object[].class).getResultList());
      assertEquals(1, rows.size());
      assertEquals(9, rows.get(0)[0]);
      assertEquals(new BigDecimal("770.00"), rows.get(0)[1]);
    }
  }

  @Test
  void rollbackDoesNotUndoAnotherTransaction() {
    try (EntityManagerFactory emf = Database.inMemory(false)) {
      EntityManager em = emf.createEntityManager();
      em.getTransaction().begin();
      emf.runInTransaction(other ->                     // code under test commits on its own
          other.persist(new Invoice("Maria", "90.00", LocalDate.of(2026, 9, 25))));
      em.getTransaction().rollback();
      em.close();
      assertEquals(4, clients(emf).size());            // Maria is still there
    }
  }
}
