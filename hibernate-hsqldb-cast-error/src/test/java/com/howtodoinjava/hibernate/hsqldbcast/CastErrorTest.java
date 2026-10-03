package com.howtodoinjava.hibernate.hsqldbcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.hibernate.exception.DataException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.query.QueryArgumentException;
import org.hibernate.query.SemanticException;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hsqldb.HsqlException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CastErrorTest {

  private static final String CAST = "data exception: invalid character value for cast";
  private static int counter;

  private EntityManagerFactory emf;

  @BeforeEach
  void setUp() {
    emf = Database.create(Database.hsqldb("passes" + (++counter)), Action.CREATE_DROP, false, BusPass.class);
    CastErrorDemo.seed(emf);
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private List<BusPass> nativeQuery(EntityManagerFactory factory, String sql, Object... params) {
    return factory.callInTransaction(em -> {
      var query = em.createNativeQuery(sql, BusPass.class);
      for (int i = 0; i < params.length; i++) {
        query.setParameter(i + 1, params[i]);
      }
      @SuppressWarnings("unchecked")
      List<BusPass> list = query.getResultList();
      return list;
    });
  }

  private static List<String> numbers(List<BusPass> passes) {
    return passes.stream().map(BusPass::getPassNumber).toList();
  }

  private static void assertHsqlCast(DataException e) {
    assertInstanceOf(SQLDataException.class, e.getSQLException());
    assertEquals("22018", e.getSQLState());
    assertEquals(-3438, e.getErrorCode());
    assertInstanceOf(HsqlException.class, e.getSQLException().getCause());
    assertEquals(CAST, e.getSQLException().getCause().getMessage());
    assertInstanceOf(PersistenceException.class, e);
  }

  // ---- Cause 1: a String for a numeric column

  @Test
  void stringParameterForNumericColumnFailsWhileBinding() {
    DataException e = assertThrows(DataException.class, () ->
        nativeQuery(emf, "select * from bus_pass where zone = ?", "A12"));
    assertEquals("JDBC exception executing SQL [" + CAST + "] [select * from bus_pass where zone = ?]",
        e.getMessage());
    assertHsqlCast(e);
    StackTraceElement top = e.getSQLException().getStackTrace()[2];
    assertEquals("setParameter", top.getMethodName());
    SQLException root = RootCause.sqlException(e);
    assertEquals("22018", root.getSQLState());
    assertEquals(CAST, root.getMessage());
  }

  @Test
  void stringLiteralForNumericColumnFailsWhilePreparing() {
    DataException e = assertThrows(DataException.class, () ->
        nativeQuery(emf, "select * from bus_pass where zone = 'A12'"));
    assertEquals("Could not prepare statement [" + CAST
        + " in statement [select * from bus_pass where zone = 'A12']] [select * from bus_pass where zone = 'A12']",
        e.getMessage());
    assertHsqlCast(e);
  }

  @Test
  void numericStringIsConvertedSilently() {
    assertEquals(List.of("A12"), numbers(nativeQuery(emf, "select * from bus_pass where zone = ?", "2")));
  }

  @Test
  void hqlRejectsStringArgumentBeforeSql() {
    QueryArgumentException e = assertThrows(QueryArgumentException.class, () -> emf.runInTransaction(em ->
        em.createQuery("from BusPass p where p.zone = :zone", BusPass.class).setParameter("zone", "A12")));
    assertEquals("Argument to query parameter has an incompatible type: Error coercing value "
        + "(argument [A12] is not assignable to java.lang.Integer)", e.getMessage());
    assertInstanceOf(IllegalArgumentException.class, e);
  }

  @Test
  void hqlRejectsStringLiteral() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> emf.runInTransaction(em ->
        em.createQuery("from BusPass p where p.zone = 'A12'", BusPass.class)));
    assertInstanceOf(SemanticException.class, e.getCause());
    assertEquals("Cannot compare left expression of type 'java.lang.Integer' with right expression of type "
        + "'java.lang.String'", e.getCause().getMessage());
  }

  @Test
  void fixQueryTheRightColumnOrBindAnInteger() {
    assertEquals(List.of("A12"), numbers(nativeQuery(emf, "select * from bus_pass where passNumber = ?", "A12")));
    assertEquals(List.of("A12"), numbers(nativeQuery(emf, "select * from bus_pass where zone = ?", 2)));
    assertEquals(List.of("B7"), numbers(emf.callInTransaction(em ->
        em.createQuery("from BusPass p where p.zone = :zone", BusPass.class)
            .setParameter("zone", 3).getResultList())));
    NumberFormatException e = assertThrows(NumberFormatException.class, () -> Integer.parseInt("A12"));
    assertEquals("For input string: \"A12\"", e.getMessage());
  }

  // ---- Cause 2: enum mapping does not match the column

  private static final String ORDINAL_TABLE = CastErrorDemo.ORDINAL_TABLE;
  private static final String VARCHAR_TABLE = ORDINAL_TABLE.replace("type tinyint)", "type varchar(255))");

  @Test
  void stringEnumIntoSmallintColumnFailsWhileBinding() {
    String url = Database.hsqldb("tinyint" + (++counter));
    Database.execute(url, ORDINAL_TABLE);
    try (EntityManagerFactory legacy = Database.create(url, Action.NONE, false, BusPass.class)) {
      DataException e = assertThrows(DataException.class, () -> legacy.runInTransaction(em ->
          em.persist(new BusPass("C3", "Maria", 1, LocalDate.of(2026, 12, 31), PassType.STUDENT))));
      assertEquals("Unable to bind parameter #3 - STUDENT [" + CAST + "] [n/a]", e.getMessage());
      assertHsqlCast(e);
    }
  }

  @Test
  void schemaValidationReportsTheWrongColumnType() {
    String url = Database.hsqldb("validate" + (++counter));
    Database.execute(url, ORDINAL_TABLE);
    PersistenceException e = assertThrows(PersistenceException.class, () ->
        Database.create(url, Action.VALIDATE, false, BusPass.class));
    assertEquals("Unable to build Hibernate SessionFactory  [persistence unit: bus-passes] ", e.getMessage());
    assertInstanceOf(SchemaManagementException.class, e.getCause());
    assertEquals(CastErrorDemo.VALIDATION_MESSAGE, e.getCause().getMessage());
  }

  @Test
  void fixMigrateOrdinalsToNames() {
    String url = Database.hsqldb("migrate" + (++counter));
    Database.execute(url, ORDINAL_TABLE,
        "insert into bus_pass (passNumber, holderName, zone, validUntil, type) values ('A12', 'Lokesh', 2, '2026-12-31', 0)");
    Database.execute(url, CastErrorDemo.MIGRATION);
    try (EntityManagerFactory fixed = Database.create(url, Action.VALIDATE, false, BusPass.class)) {
      fixed.runInTransaction(em ->
          em.persist(new BusPass("C3", "Maria", 1, LocalDate.of(2026, 12, 31), PassType.STUDENT)));
      List<PassType> types = fixed.callInTransaction(em ->
          em.createQuery("select p.type from BusPass p order by p.id", PassType.class).getResultList());
      assertEquals(List.of(PassType.MONTHLY, PassType.STUDENT), types);
    }
  }

  @Test
  void fixKeepTheOrdinalMapping() {
    String url = Database.hsqldb("keepordinal" + (++counter));
    Database.execute(url, ORDINAL_TABLE,
        "insert into bus_pass (passNumber, holderName, zone, validUntil, type) values ('A12', 'Lokesh', 2, '2026-12-31', 0)");
    try (EntityManagerFactory ordinal = Database.create(url, Action.NONE, false, OrdinalBusPass.class)) {
      ordinal.runInTransaction(em ->
          em.persist(new OrdinalBusPass("C3", "Maria", 1, LocalDate.of(2026, 12, 31), PassType.STUDENT)));
      List<PassType> types = ordinal.callInTransaction(em ->
          em.createQuery("select p.type from OrdinalBusPass p order by p.id", PassType.class).getResultList());
      assertEquals(List.of(PassType.MONTHLY, PassType.STUDENT), types);
      Number stored = ordinal.callInTransaction(em -> (Number) em.createNativeQuery(
          "select type from bus_pass where passNumber = 'C3'").getSingleResult());
      assertEquals(2, stored.intValue());
    }
  }

  @Test
  void ordinalEnumOverNamesFailsWhileReading() {
    String url = Database.hsqldb("varchar" + (++counter));
    Database.execute(url, VARCHAR_TABLE,
        "insert into bus_pass (passNumber, holderName, zone, validUntil, type) values ('A12', 'Lokesh', 2, '2026-12-31', 'MONTHLY')");
    try (EntityManagerFactory legacy = Database.create(url, Action.NONE, false, OrdinalBusPass.class)) {
      SQLGrammarException e = assertThrows(SQLGrammarException.class, () -> legacy.runInTransaction(em ->
          em.createQuery("from OrdinalBusPass", OrdinalBusPass.class).getResultList()));
      assertEquals("Could not extract column [4] from JDBC ResultSet [incompatible data type in conversion: "
          + "from SQL type VARCHAR to java.lang.Integer, value: MONTHLY] [n/a]", e.getMessage());
      assertEquals("42561", e.getSQLState());

      // writing an ordinal into the varchar column works and hides the problem
      legacy.runInTransaction(em ->
          em.persist(new OrdinalBusPass("B7", "Alex", 3, LocalDate.of(2026, 10, 31), PassType.WEEKLY)));
      String stored = legacy.callInTransaction(em -> (String) em.createNativeQuery(
          "select type from bus_pass where passNumber = 'B7'").getSingleResult());
      assertEquals("1", stored);
    }
    try (EntityManagerFactory fixed = Database.create(url, Action.NONE, false, BusPass.class)) {
      assertEquals(PassType.MONTHLY, fixed.callInTransaction(em ->
          em.createQuery("select p.type from BusPass p where p.passNumber = 'A12'", PassType.class)
              .getSingleResult()));
    }
  }

  // ---- Cause 3: a date string in the wrong format

  @Test
  void dateStringInWrongFormatFails() {
    DataException e = assertThrows(DataException.class, () ->
        nativeQuery(emf, "select * from bus_pass where validUntil < ?", "30/11/2026"));
    assertEquals("JDBC exception executing SQL [data exception: invalid datetime format] "
        + "[select * from bus_pass where validUntil < ?]", e.getMessage());
    assertEquals("22007", e.getSQLState());
    assertEquals(-3407, e.getErrorCode());

    DataException literal = assertThrows(DataException.class, () ->
        nativeQuery(emf, "select * from bus_pass where validUntil < '30/11/2026'"));
    assertTrue(literal.getMessage().startsWith("Could not prepare statement [data exception: invalid datetime format"));
  }

  @Test
  void fixParseTheDateInJava() {
    assertEquals(List.of("B7"), numbers(nativeQuery(emf, "select * from bus_pass where validUntil < ?", "2026-11-30")));
    LocalDate until = LocalDate.parse("30/11/2026", DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    assertEquals(List.of("B7"), numbers(nativeQuery(emf, "select * from bus_pass where validUntil < ?", until)));
    QueryArgumentException e = assertThrows(QueryArgumentException.class, () -> emf.runInTransaction(em ->
        em.createQuery("from BusPass p where p.validUntil < :until", BusPass.class)
            .setParameter("until", "30/11/2026")));
    assertEquals("Argument to parameter named 'until' has an incompatible type "
        + "(argument [30/11/2026] is not assignable to java.time.LocalDate)", e.getMessage());
  }

  // ---- Cause 4: values in the wrong order (INSERT and stored procedure)

  @Test
  void insertWithValuesInWrongOrderFails() {
    DataException e = assertThrows(DataException.class, () -> emf.runInTransaction(em ->
        em.createNativeQuery(CastErrorDemo.WRONG_INSERT)
            .setParameter(1, "C3").setParameter(2, "Maria").setParameter(3, 1)
            .executeUpdate()));
    assertEquals("JDBC exception executing SQL [" + CAST + "] [" + CastErrorDemo.WRONG_INSERT + "]", e.getMessage());
    assertHsqlCast(e);
  }

  @Test
  void storedProcedureWithValuesInWrongOrderFails() {
    String url = Database.hsqldb("procedure" + (++counter));
    try (EntityManagerFactory factory = Database.create(url, Action.CREATE_DROP, false, BusPass.class)) {
      Database.execute(url, CastErrorDemo.WRONG_PROCEDURE);
      DataException e = assertThrows(DataException.class, () -> factory.runInTransaction(em ->
          CastErrorDemo.addBusPass(em, "C3", "Maria", 1)));
      assertTrue(e.getMessage().startsWith("Error calling CallableStatement.getMoreResults [" + CAST + "]"));
      assertHsqlCast(e);
    }
  }

  @Test
  void fixNameTheColumnsInTheSameOrderAsTheValues() {
    String url = Database.hsqldb("procedurefix" + (++counter));
    try (EntityManagerFactory factory = Database.create(url, Action.CREATE_DROP, false, BusPass.class)) {
      Database.execute(url, CastErrorDemo.FIXED_PROCEDURE);
      factory.runInTransaction(em -> CastErrorDemo.addBusPass(em, "C3", "Maria", 1));
      factory.runInTransaction(em -> em.createNativeQuery(CastErrorDemo.FIXED_INSERT)
          .setParameter(1, "D4").setParameter(2, "Maria").setParameter(3, 1).executeUpdate());
      List<Object[]> rows = factory.callInTransaction(em -> em.createQuery(
          "select p.passNumber, p.holderName, p.zone from BusPass p order by p.id", Object[].class).getResultList());
      assertEquals("[C3, Maria, 1] [D4, Maria, 1]",
          rows.stream().map(java.util.Arrays::toString).reduce((a, b) -> a + " " + b).orElseThrow());
    }
  }

  // ---- The same mistakes on H2

  @Test
  void h2MessagesForTheSameMistakes() {
    try (EntityManagerFactory h2 = Database.create(Database.h2("passes" + (++counter)), Action.CREATE_DROP, false,
        BusPass.class)) {
      CastErrorDemo.seed(h2);
      assertEquals("Data conversion error converting \"CHARACTER VARYING to DECFLOAT\"", h2Message(() ->
          nativeQuery(h2, "select * from bus_pass where zone = ?", "A12")));
      assertEquals("Data conversion error converting \"A12\"", h2Message(() ->
          nativeQuery(h2, "select * from bus_pass where zone = 'A12'")));
      assertEquals("Cannot parse \"DATE\" constant \"30/11/2026\"", h2Message(() ->
          nativeQuery(h2, "select * from bus_pass where validUntil < ?", "30/11/2026")));
      assertEquals("Data conversion error converting \"'Maria' (BUS_PASS: \"\"ZONE\"\" INTEGER NOT NULL)\"",
          h2Message(() -> h2.runInTransaction(em -> em.createNativeQuery(CastErrorDemo.WRONG_INSERT)
              .setParameter(1, "C3").setParameter(2, "Maria").setParameter(3, 1).executeUpdate())));
    }
    String url = Database.h2("tinyint" + (++counter));
    Database.execute(url, ORDINAL_TABLE);
    try (EntityManagerFactory legacy = Database.create(url, Action.NONE, false, BusPass.class)) {
      assertEquals("Data conversion error converting \"'STUDENT' (BUS_PASS: \"\"TYPE\"\" TINYINT)\"",
          h2Message(() -> legacy.runInTransaction(em ->
              em.persist(new BusPass("C3", "Maria", 1, LocalDate.of(2026, 12, 31), PassType.STUDENT)))));
    }
    String varcharUrl = Database.h2("varchar" + (++counter));
    Database.execute(varcharUrl, VARCHAR_TABLE,
        "insert into bus_pass (passNumber, holderName, zone, validUntil, type) values ('A12', 'Lokesh', 2, '2026-12-31', 'MONTHLY')");
    try (EntityManagerFactory legacy = Database.create(varcharUrl, Action.NONE, false, OrdinalBusPass.class)) {
      DataException e = assertThrows(DataException.class, () -> legacy.runInTransaction(em ->
          em.createQuery("from OrdinalBusPass", OrdinalBusPass.class).getResultList()));
      assertEquals("Data conversion error converting \"MONTHLY\" [22018-252]", e.getSQLException().getMessage());
    }
  }

  private static String h2Message(Runnable action) {
    DataException e = assertThrows(DataException.class, action::run);
    SQLException sql = e.getSQLException();
    assertTrue(sql.getSQLState().equals("22018") || sql.getSQLState().equals("22007"));
    return sql.getMessage().substring(0, sql.getMessage().indexOf(';'));
  }
}
