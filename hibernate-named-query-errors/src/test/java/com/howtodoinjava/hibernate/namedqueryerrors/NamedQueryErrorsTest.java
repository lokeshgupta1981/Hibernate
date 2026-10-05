package com.howtodoinjava.hibernate.namedqueryerrors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PersistenceException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.hibernate.QueryParameterException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.query.NamedQueryValidationException;
import org.hibernate.query.sqm.UnknownPathException;
import org.junit.jupiter.api.Test;

class NamedQueryErrorsTest {

  /** The fix for ?0 and ?: number the parameter from ?1. */
  @NamedQuery(name = "Shift.findMinHours", query = "select s from Shift s where s.hours >= ?1")
  static class OrdinalParameterFix {
  }

  /** Bootstraps the persistence unit and returns the NamedQueryValidationException. */
  private static NamedQueryValidationException startupError(Class<?>... classes) {
    PersistenceException e = assertThrows(PersistenceException.class,
        () -> Database.base(false).managedClasses(classes).createEntityManagerFactory().close());
    assertTrue(e.getMessage().startsWith(
        "Unable to build Hibernate SessionFactory  [persistence unit: volunteer-shifts]"));
    return assertInstanceOf(NamedQueryValidationException.class, e.getCause());
  }

  private static NamedQueryValidationException startupError(Class<?> broken) {
    return startupError(Volunteer.class, Shift.class, broken);
  }

  private static List<String> names(List<Shift> shifts) {
    return shifts.stream().map(Shift::getEventName).toList();
  }

  @Test
  void fixedQueriesStartAndReturnShifts() {
    try (EntityManagerFactory emf = Database.create(false)) {
      Database.seed(emf);
      emf.runInTransaction(em -> {
        assertEquals(List.of("Food Drive", "Book Fair"), names(em
            .createNamedQuery("Shift.findLong", Shift.class)
            .setParameter("hours", 4).getResultList()));
        assertEquals(List.of("Food Drive", "Book Fair"), names(em
            .createNamedQuery("Shift.findByVolunteer", Shift.class)
            .setParameter("name", "Lokesh").getResultList()));
        assertEquals(List.of("Park Cleanup", "Book Fair"), names(em
            .createNamedQuery("Shift.findAfter", Shift.class)
            .setParameter("from", LocalDateTime.of(2026, 10, 11, 0, 0)).getResultList()));
        assertEquals(List.of("Food Drive", "Book Fair"), names(em
            .createNamedQuery("Shift.findLongSql", Shift.class)
            .setParameter("hours", 4).getResultList()));
      });
    }
  }

  @Test
  void tableNameInsteadOfEntityName() {
    NamedQueryValidationException e = startupError(BrokenQueries.TableName.class);
    assertTrue(e.getMessage().startsWith("Errors in named queries: "));
    assertTrue(e.getMessage().contains(
        "[1] Error in query named 'Shift.findAll': Could not resolve root entity 'volunteer_shift'"));
    assertEquals(Set.of("Shift.findAll"), e.getErrors().keySet());
  }

  @Test
  void columnNameInsteadOfAttributeName() {
    NamedQueryValidationException e = startupError(BrokenQueries.ColumnName.class);
    assertTrue(e.getMessage().contains("Error in query named 'Shift.findStartingAfter': "
        + "Could not resolve attribute 'starts_at' of "
        + "'com.howtodoinjava.hibernate.namedqueryerrors.Shift' "
        + "[select s from Shift s where s.starts_at > :from]"));
    assertInstanceOf(UnknownPathException.class, e.getErrors().get("Shift.findStartingAfter"));
  }

  @Test
  void misspelledAttributeOfJoinedEntity() {
    NamedQueryValidationException e = startupError(BrokenQueries.AttributeTypo.class);
    assertTrue(e.getMessage().contains("Error in query named 'Shift.findByVolunteerName': "
        + "Could not resolve attribute 'nmae' of "
        + "'com.howtodoinjava.hibernate.namedqueryerrors.Volunteer'"));
  }

  @Test
  void entityNotRegistered() {
    NamedQueryValidationException e =
        startupError(Volunteer.class, BrokenQueries.UnregisteredEntity.class);
    assertTrue(e.getMessage().contains(
        "Error in query named 'Shift.findAll': Could not resolve root entity 'Shift'"));

    // Fix: register Shift
    try (EntityManagerFactory emf = Database.base(false)
        .managedClasses(Volunteer.class, Shift.class, BrokenQueries.UnregisteredEntity.class)
        .createEntityManagerFactory()) {
      assertTrue(emf.isOpen());
    }
  }

  @Test
  void sqlInJpqlNamedQuery() {
    NamedQueryValidationException e = startupError(BrokenQueries.SqlInJpql.class);
    assertTrue(e.getMessage().contains("Error in query named 'Shift.findLongShifts': "
        + "At 1:7 and token '*', extraneous input '*' expecting {"));
    assertTrue(e.getMessage().endsWith("[select * from volunteer_shift where hours >= 4]"));
  }

  @Test
  void keywordTypo() {
    NamedQueryValidationException e = startupError(BrokenQueries.KeywordTypo.class);
    assertTrue(e.getMessage().contains("Error in query named 'Shift.findAll': "
        + "At 1:14 and token 'Shift', mismatched input 'Shift', expecting one of the following "
        + "tokens: <EOF>, ',', EXCEPT, FETCH, FROM, GROUP, HAVING, INTERSECT, LIMIT, OFFSET, ORDER, "
        + "UNION, WHERE [select s form Shift s]"));
  }

  @Test
  void trailingSemicolon() {
    NamedQueryValidationException e = startupError(BrokenQueries.TrailingSemicolon.class);
    assertTrue(e.getMessage().contains("Error in query named 'Shift.findMinHours': "
        + "At 1:45 and token ';', extraneous input ';' expecting <EOF> "
        + "[select s from Shift s where s.hours >= :hours;]"));
  }

  @Test
  void unlabeledParameter() {
    NamedQueryValidationException e = startupError(BrokenQueries.UnlabeledParameter.class);
    assertTrue(e.getMessage().contains("Error in query named 'Shift.findMinHours': "
        + "Unlabeled ordinal parameter ('?' rather than ?1)"));
  }

  @Test
  void parameterStartingAtZero() {
    NamedQueryValidationException e = startupError(BrokenQueries.ZeroParameter.class);
    assertTrue(e.getMessage().contains("Error in query named 'Shift.findMinHours': "
        + "Ordinal parameter labels start from '?0' (ordinal parameters must be labelled from '?1')"));
  }

  @Test
  void ordinalParameterFromOneWorks() {
    try (EntityManagerFactory emf = Database.create(false, OrdinalParameterFix.class)) {
      Database.seed(emf);
      List<Shift> shifts = emf.callInTransaction(em -> em
          .createNamedQuery("Shift.findMinHours", Shift.class)
          .setParameter(1, 4)
          .getResultList());
      assertEquals(List.of("Food Drive", "Book Fair"), names(shifts));
    }
  }

  @Test
  void literalOfWrongType() {
    NamedQueryValidationException e = startupError(BrokenQueries.TypeMismatch.class);
    assertTrue(e.getMessage().contains("Error in query named 'Shift.findFourHours': "
        + "Cannot compare left expression of type 'java.lang.Integer' "
        + "with right expression of type 'java.lang.String'"));
  }

  @Test
  void allBrokenQueriesAreReportedTogether() {
    NamedQueryValidationException e = startupError(BrokenQueries.TwoErrors.class);
    assertEquals(Set.of("Shift.findAll", "Shift.findStartingAfter"), e.getErrors().keySet());
    assertTrue(e.getMessage().contains("[1] Error in query named"));
    assertTrue(e.getMessage().contains("[2] Error in query named"));
  }

  @Test
  void startupCheckOffMovesTheErrorToCreateNamedQuery() {
    try (EntityManagerFactory emf = Database.configuration(false, BrokenQueries.ColumnName.class)
        .property("hibernate.query.startup_check", false)
        .createEntityManagerFactory()) {
      assertTrue(emf.isOpen());
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.createNamedQuery("Shift.findStartingAfter", Shift.class)));
      assertEquals("org.hibernate.query.sqm.UnknownPathException: Could not resolve attribute "
          + "'starts_at' of 'com.howtodoinjava.hibernate.namedqueryerrors.Shift' "
          + "[select s from Shift s where s.starts_at > :from]", e.getMessage());
    }
  }

  @Test
  void namedNativeQueryIsNotCheckedAtStartup() {
    try (EntityManagerFactory emf = Database.create(false, BrokenQueries.NativeWrongTable.class)) {
      assertTrue(emf.isOpen());
      SQLGrammarException e = assertThrows(SQLGrammarException.class, () -> emf.runInTransaction(
          em -> em.createNamedQuery("Shift.findAllSql", Shift.class).getResultList()));
      assertTrue(e.getMessage().startsWith(
          "Could not prepare statement [Table \"SHIFTS\" not found; SQL statement:"));
    }
  }

  @Test
  void wrongParameterNameFailsOnlyWhenTheQueryRuns() {
    try (EntityManagerFactory emf = Database.create(false)) {
      IllegalArgumentException wrongName = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.createNamedQuery("Shift.findLong", Shift.class)
              .setParameter("hour", 4)));
      assertEquals("No parameter named ':hour' in query with named parameters [hours]",
          wrongName.getMessage());

      QueryParameterException missing = assertThrows(QueryParameterException.class,
          () -> emf.runInTransaction(em -> em.createNamedQuery("Shift.findLong", Shift.class)
              .getResultList()));
      assertEquals("No argument for named parameter ':hours'", missing.getMessage());
    }
  }

  @Test
  void hibernateUtilPrintsInitialSessionFactoryCreationFailed() {
    PrintStream originalErr = System.err;
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    System.setErr(new PrintStream(err, true));
    try {
      ExceptionInInitializerError first =
          assertThrows(ExceptionInInitializerError.class, HibernateUtil::getSessionFactory);
      NamedQueryValidationException cause =
          assertInstanceOf(NamedQueryValidationException.class, first.getCause());
      assertTrue(cause.getMessage().contains(
          "[1] Error in query named 'Shift.findAll': Could not resolve root entity 'volunteer_shift'"));

      NoClassDefFoundError second =
          assertThrows(NoClassDefFoundError.class, HibernateUtil::getSessionFactory);
      assertEquals("Could not initialize class "
          + "com.howtodoinjava.hibernate.namedqueryerrors.HibernateUtil", second.getMessage());
    } finally {
      System.setErr(originalErr);
    }
    assertTrue(err.toString().contains("Initial SessionFactory creation failed."
        + "org.hibernate.query.NamedQueryValidationException: Errors in named queries: "));
  }
}
