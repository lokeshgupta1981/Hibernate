package com.howtodoinjava.hibernate.genericjdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.JDBCException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.Test;

class GenericJdbcExceptionTest {

  private static final LocalDate PICKUP = LocalDate.of(2026, 10, 10);

  private static CakeOrder chocolateCake() {
    return new CakeOrder("Lokesh", "chocolate", PICKUP, new BigDecimal("40.00"));
  }

  private static Order lemonOrder() {
    return new Order("Lokesh", "lemon", PICKUP, new BigDecimal("35.00"));
  }

  private static long countCakeOrders(EntityManagerFactory emf) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from CakeOrder", Long.class).getSingleResult());
  }

  // ---- 1. Missing table: GenericJDBCException on SQLite, SQLGrammarException on H2

  @Test
  void sqliteMissingTableThrowsGenericJdbcException() {
    try (EntityManagerFactory emf = Database.sqlite("noschema", Action.NONE, false, CakeOrder.class)
        .createEntityManagerFactory()) {
      GenericJDBCException e = assertThrows(GenericJDBCException.class, () -> emf.runInTransaction(em ->
          em.createQuery("from CakeOrder", CakeOrder.class).getResultList()));
      assertEquals("Could not prepare statement [[SQLITE_ERROR] SQL error or missing database "
          + "(no such table: cake_order)] [select co1_0.id,co1_0.customerName,co1_0.flavor,"
          + "co1_0.pickupDate,co1_0.order_value from cake_order co1_0]", e.getMessage());
      assertInstanceOf(PersistenceException.class, e);
      assertNull(e.getSQLException().getSQLState());
      assertEquals(1, e.getErrorCode());
      assertEquals("org.sqlite.SQLiteException", e.getSQLException().getClass().getName());
    }
  }

  @Test
  void h2MissingTableThrowsSqlGrammarException() {
    try (EntityManagerFactory emf = Database.h2("noschema", Action.NONE, false, CakeOrder.class)
        .createEntityManagerFactory()) {
      SQLGrammarException e = assertThrows(SQLGrammarException.class, () -> emf.runInTransaction(em ->
          em.createQuery("from CakeOrder", CakeOrder.class).getResultList()));
      assertTrue(e.getMessage().startsWith("Could not prepare statement [Table \"CAKE_ORDER\" not found;"));
      assertEquals("42S02", e.getSQLState());
      assertEquals(42102, e.getErrorCode());
      assertInstanceOf(java.sql.SQLSyntaxErrorException.class, e.getSQLException());
    }
  }

  @Test
  void h2PersistWithoutSchemaFailsOnTheSequence() {
    try (EntityManagerFactory emf = Database.h2("noschema2", Action.NONE, false, CakeOrder.class)
        .createEntityManagerFactory()) {
      SQLGrammarException e = assertThrows(SQLGrammarException.class, () ->
          emf.runInTransaction(em -> em.persist(chocolateCake())));
      assertTrue(e.getMessage().startsWith("Could not prepare statement [Sequence \"CAKE_ORDER_SEQ\" not found;"));
      assertEquals("select next value for cake_order_SEQ", e.getSQL());
    }
  }

  @Test
  void creatingTheSchemaFixesSqliteAndH2() {
    try (EntityManagerFactory sqlite = Database.sqlite("withschema", Action.CREATE_DROP, false, CakeOrder.class)
        .createEntityManagerFactory();
        EntityManagerFactory h2 = Database.h2("withschema", Action.CREATE_DROP, false, CakeOrder.class)
            .createEntityManagerFactory()) {
      sqlite.runInTransaction(em -> em.persist(chocolateCake()));
      h2.runInTransaction(em -> em.persist(chocolateCake()));
      assertEquals(1, countCakeOrders(sqlite));
      assertEquals(1, countCakeOrders(h2));
    }
  }

  @Test
  void hbm2ddlAutoPropertyCreatesTheSchema() {
    try (EntityManagerFactory emf = Database.h2("hbm2ddl", Action.NONE, false, CakeOrder.class)
        .property("hibernate.hbm2ddl.auto", "create-drop")
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(chocolateCake()));
      assertEquals(1, countCakeOrders(emf));
    }
  }

  // ---- 2. Missing column

  private static void createOldTableWithoutPickupDate(String db) throws SQLException {
    try (Connection c = DriverManager.getConnection("jdbc:h2:mem:" + db + ";DB_CLOSE_DELAY=-1", "sa", "")) {
      c.createStatement().execute("create table cake_order (id bigint primary key, "
          + "customerName varchar(255), flavor varchar(255), order_value numeric(38,2))");
      c.createStatement().execute("create sequence cake_order_SEQ start with 1 increment by 50");
    }
  }

  @Test
  void missingColumnThrowsSqlGrammarException() throws SQLException {
    createOldTableWithoutPickupDate("oldschema");
    try (EntityManagerFactory emf = Database.h2("oldschema", Action.NONE, false, CakeOrder.class)
        .createEntityManagerFactory()) {
      RollbackException e = assertThrows(RollbackException.class, () ->
          emf.runInTransaction(em -> em.persist(chocolateCake())));
      SQLGrammarException grammar = assertInstanceOf(SQLGrammarException.class, e.getCause());
      assertTrue(grammar.getMessage().startsWith("Could not prepare statement [Column \"PICKUPDATE\" not found;"));
      assertEquals("42S22", grammar.getSQLState());
      assertEquals(42122, grammar.getErrorCode());
    }
  }

  @Test
  void validateReportsTheMissingColumnAtStartup() throws SQLException {
    createOldTableWithoutPickupDate("validate");
    PersistenceException e = assertThrows(PersistenceException.class, () ->
        Database.h2("validate", Action.VALIDATE, false, CakeOrder.class).createEntityManagerFactory());
    SchemaManagementException cause = assertInstanceOf(SchemaManagementException.class, e.getCause());
    assertEquals("Schema validation: missing column [pickupDate] in table [cake_order]", cause.getMessage());
  }

  // ---- 3. Reserved words

  @Test
  void tableNamedOrderFailsOnInsert() {
    try (EntityManagerFactory emf = Database.h2("reserved", Action.CREATE_DROP, false, Order.class)
        .createEntityManagerFactory()) {
      RollbackException e = assertThrows(RollbackException.class, () ->
          emf.runInTransaction(em -> em.persist(lemonOrder())));
      SQLGrammarException grammar = assertInstanceOf(SQLGrammarException.class, e.getCause());
      assertTrue(grammar.getMessage().startsWith(
          "Could not prepare statement [Syntax error in SQL statement \"insert into [*]Order "
              + "(customerName,flavor,pickupDate,value,id) values (?,?,?,?,?)\"; expected \"identifier\";"));
      assertEquals("42001", grammar.getSQLState());
    }
  }

  @Test
  void tableNamedOrderOnSqliteThrowsGenericJdbcException() {
    try (EntityManagerFactory emf = Database.sqlite("reserved", Action.CREATE_DROP, false, Order.class)
        .createEntityManagerFactory()) {
      PersistenceException e = assertThrows(PersistenceException.class, () ->
          emf.runInTransaction(em -> em.persist(lemonOrder())));
      JDBCException jdbc = RootCause.jdbcException(e);
      assertInstanceOf(GenericJDBCException.class, jdbc);
      assertEquals("Could not prepare statement [[SQLITE_ERROR] SQL error or missing database "
          + "(near \"Order\": syntax error)] "
          + "[insert into Order (customerName,flavor,pickupDate,value,id) values (?,?,?,?,?)]", jdbc.getMessage());
    }
  }

  @Test
  void haltOnErrorStopsAtTheFailingDdl() {
    PersistenceException e = assertThrows(PersistenceException.class, () ->
        Database.h2("halt", Action.CREATE_DROP, false, Order.class)
            .property("hibernate.hbm2ddl.halt_on_error", "true")
            .createEntityManagerFactory());
    SchemaManagementException cause = assertInstanceOf(SchemaManagementException.class, e.getCause());
    assertTrue(cause.getMessage().startsWith(
        "Halting on error : Error executing DDL \"drop table if exists Order cascade \" via JDBC "
            + "[Syntax error in SQL statement \"drop table if exists [*]Order cascade \"; expected \"identifier\";]"));
  }

  @Test
  void autoQuoteKeywordQuotesOrderAndValue() {
    try (EntityManagerFactory emf = Database.h2("autoquote", Action.CREATE_DROP, false, Order.class)
        .property("hibernate.auto_quote_keyword", "true")
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(lemonOrder()));
      long count = emf.callInTransaction(em ->
          em.createQuery("select count(*) from Order", Long.class).getSingleResult());
      assertEquals(1, count);
      List<?> tables = emf.callInTransaction(em -> em.createNativeQuery(
          "select column_name from information_schema.columns where table_name = 'Order' and column_name = 'value'")
          .getResultList());
      assertEquals(List.of("value"), tables);
    }
  }

  @Test
  void globallyQuotedIdentifiersFixOrderButBreakUnquotedNativeSql() {
    try (EntityManagerFactory emf = Database.h2("globalquote", Action.CREATE_DROP, false, Order.class, CakeOrder.class)
        .property("hibernate.globally_quoted_identifiers", "true")
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(lemonOrder()));
      emf.runInTransaction(em -> em.persist(chocolateCake()));
      assertEquals(1, countCakeOrders(emf));
      SQLGrammarException e = assertThrows(SQLGrammarException.class, () -> emf.runInTransaction(em ->
          em.createNativeQuery("select flavor from cake_order").getResultList()));
      assertTrue(e.getMessage().startsWith(
          "Could not prepare statement [Table \"CAKE_ORDER\" not found (candidates are: \"cake_order\");"));
      List<?> flavors = emf.callInTransaction(em ->
          em.createNativeQuery("select \"flavor\" from \"cake_order\"").getResultList());
      assertEquals(List.of("chocolate"), flavors);
    }
  }

  @Test
  void renamedTableAndColumnWork() {
    try (EntityManagerFactory emf = Database.h2("renamed", Action.CREATE_DROP, false, CakeOrder.class)
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(chocolateCake()));
      List<?> values = emf.callInTransaction(em ->
          em.createNativeQuery("select order_value from cake_order").getResultList());
      assertEquals(new BigDecimal("40.00"), values.get(0));
    }
  }

  // ---- 4. Wrong dialect and database-specific SQL

  @Test
  void oracleDialectOnH2GeneratesOracleSql() {
    try (EntityManagerFactory emf = Database.h2("oracle", Action.CREATE_DROP, false, CakeOrder.class)
        .property("hibernate.dialect", "org.hibernate.dialect.OracleDialect")
        .createEntityManagerFactory()) {
      SQLGrammarException e = assertThrows(SQLGrammarException.class, () ->
          emf.runInTransaction(em -> em.persist(chocolateCake())));
      assertEquals("select cake_order_SEQ.nextval from dual", e.getSQL());
      assertTrue(e.getMessage().startsWith("Could not prepare statement [Column \"CAKE_ORDER_SEQ.NEXTVAL\" not found;"));
    }
  }

  @Test
  void sqlServerDialectOnH2GeneratesCountBig() {
    try (EntityManagerFactory emf = Database.h2("sqlserver", Action.CREATE_DROP, false, CakeOrder.class)
        .property("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect")
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(chocolateCake()));
      SQLGrammarException e = assertThrows(SQLGrammarException.class, () -> countCakeOrders(emf));
      assertEquals("select count_big(*) from cake_order co1_0", e.getSQL());
      assertTrue(e.getMessage().startsWith("Could not prepare statement [Function \"COUNT_BIG\" not found;"));
    }
  }

  @Test
  void mySqlFunctionInNativeQueryFailsOnH2EvenInMySqlMode() {
    for (String db : new String[] {"nativefn", "nativefn;MODE=MySQL"}) {
      try (EntityManagerFactory emf = Database.h2(db, Action.CREATE_DROP, false, CakeOrder.class)
          .createEntityManagerFactory()) {
        emf.runInTransaction(em -> em.persist(chocolateCake()));
        SQLGrammarException e = assertThrows(SQLGrammarException.class, () -> emf.runInTransaction(em ->
            em.createNativeQuery("select date_format(pickupDate, '%d.%m') from cake_order").getResultList()));
        assertTrue(e.getMessage().startsWith("Could not prepare statement [Function \"DATE_FORMAT\" not found;"));
        assertEquals("90022", e.getSQLState());
      }
    }
  }

  @Test
  void hqlFormatFunctionWorksOnEveryDialect() {
    try (EntityManagerFactory emf = Database.h2("hqlformat", Action.CREATE_DROP, false, CakeOrder.class)
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(chocolateCake()));
      String day = emf.callInTransaction(em -> em.createQuery(
          "select format(c.pickupDate as 'dd.MM') from CakeOrder c", String.class).getSingleResult());
      assertEquals("10.10", day);
    }
  }

  // ---- 5. Reading the root cause

  @Test
  void rootCauseShowsSqlStateAndErrorCode() {
    try (EntityManagerFactory emf = Database.h2("rootcause", Action.CREATE_DROP, false, Order.class)
        .createEntityManagerFactory()) {
      PersistenceException e = assertThrows(PersistenceException.class, () ->
          emf.runInTransaction(em -> em.persist(lemonOrder())));
      JDBCException jdbc = RootCause.jdbcException(e);
      assertNotNull(jdbc);
      assertEquals("insert into Order (customerName,flavor,pickupDate,value,id) values (?,?,?,?,?)", jdbc.getSQL());
      assertEquals("42001", jdbc.getSQLException().getSQLState());
      assertEquals(42001, jdbc.getSQLException().getErrorCode());
      assertTrue(RootCause.describe(e).startsWith("""
          Hibernate exception: SQLGrammarException
          SQL:                 insert into Order (customerName,flavor,pickupDate,value,id) values (?,?,?,?,?)
          SQLException:        org.h2.jdbc.JdbcSQLSyntaxErrorException
          SQLState:            42001
          Error code:          42001
          Message:             Syntax error in SQL statement"""));
    }
  }

  // ---- 6. Old JDBC driver (run with -Dhsqldb.groupId=hsqldb -Dhsqldb.version=1.8.0.10)

  @Test
  void hsqldbIdentityInsertDependsOnDriverVersion() throws Exception {
    try (EntityManagerFactory emf = Database.hsqldb("bakery", Action.CREATE_DROP, false, IdentityOrder.class)
        .createEntityManagerFactory()) {
      int major = emf.callInTransaction(em -> em.unwrap(org.hibernate.Session.class)
          .doReturningWork(c -> c.getMetaData().getDriverMajorVersion()));
      if (major < 2) {
        GenericJDBCException e = assertThrows(GenericJDBCException.class, () ->
            emf.runInTransaction(em -> em.persist(new IdentityOrder("lemon"))));
        assertEquals("Could not prepare statement [This function is not supported] "
            + "[insert into identity_order (flavor,id) values (?,null)]", e.getMessage());
        assertEquals("IM001", e.getSQLState());
        assertEquals(-20, e.getErrorCode());
      } else {
        emf.runInTransaction(em -> em.persist(new IdentityOrder("lemon")));
        long count = emf.callInTransaction(em ->
            em.createQuery("select count(*) from IdentityOrder", Long.class).getSingleResult());
        assertEquals(1, count);
      }
    }
  }
}
