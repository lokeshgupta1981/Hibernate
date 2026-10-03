package com.howtodoinjava.hibernate.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import java.util.List;
import org.hibernate.QueryParameterException;
import org.hibernate.engine.query.ParameterRecognitionException;
import org.hibernate.query.ParameterLabelException;
import org.hibernate.query.UnknownParameterException;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MixedParametersTest {

  private static final String NATIVE_MIX_MESSAGE =
      "Cannot mix parameter styles between JDBC-style, ordinal and named in the same query";

  private static EntityManagerFactory emf;
  private static EntityManager em;

  @BeforeAll
  static void setUp() {
    emf = Database.create(false);
    em = emf.createEntityManager();
  }

  @AfterAll
  static void tearDown() {
    em.close();
    emf.close();
  }

  private static List<Integer> numbers(List<TrainService> trains) {
    return trains.stream().map(TrainService::getTrainNumber).toList();
  }

  // ---------- 1. Stored procedures ----------

  @Test
  void procedureNamedThenPositionalThrows() {
    StoredProcedureQuery query = em.createStoredProcedureQuery("count_departures")
        .registerStoredProcedureParameter("p_from", String.class, ParameterMode.IN);
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> query.registerStoredProcedureParameter(2, Integer.class, ParameterMode.IN));
    assertEquals("Cannot mix positional parameter with named parameter registrations", e.getMessage());
  }

  @Test
  void procedurePositionalThenNamedThrows() {
    StoredProcedureQuery query = em.createStoredProcedureQuery("count_departures")
        .registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> query.registerStoredProcedureParameter("p_hour", Integer.class, ParameterMode.IN));
    assertEquals("Cannot mix named parameter with positional parameter registrations", e.getMessage());
  }

  @Test
  void procedureVariablesUsedAsNamesThrows() {
    String from = "Delhi";
    Integer hour = 7;
    StoredProcedureQuery query = em.createStoredProcedureQuery("count_departures")
        .registerStoredProcedureParameter(from, String.class, ParameterMode.IN);
    // Integer hour selects the registerStoredProcedureParameter(int position, ...) overload
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> query.registerStoredProcedureParameter(hour, Integer.class, ParameterMode.IN));
    assertEquals("Cannot mix positional parameter with named parameter registrations", e.getMessage());
  }

  @Test
  void procedureWithNamesOnly() {
    Object total = emf.callInTransaction(em -> em.createStoredProcedureQuery("count_departures")
        .registerStoredProcedureParameter("p_from", String.class, ParameterMode.IN)
        .registerStoredProcedureParameter("p_hour", Integer.class, ParameterMode.IN)
        .setParameter("p_from", "Delhi")
        .setParameter("p_hour", 7)
        .getSingleResult());
    assertEquals(2, ((Number) total).intValue());
  }

  @Test
  void procedureWithPositionsOnly() {
    Object total = emf.callInTransaction(em -> em.createStoredProcedureQuery("count_departures")
        .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
        .registerStoredProcedureParameter(2, Integer.class, ParameterMode.IN)
        .setParameter(1, "Delhi")
        .setParameter(2, 7)
        .getSingleResult());
    assertEquals(2, ((Number) total).intValue());
  }

  // ---------- 2. Native queries ----------

  @Test
  void nativeNamedAndJdbcStyleThrows() {
    ParameterRecognitionException e = assertThrows(ParameterRecognitionException.class,
        () -> em.createNativeQuery(
            "select * from TrainService where fromStation = :from and toStation = ?", TrainService.class));
    assertEquals(NATIVE_MIX_MESSAGE, e.getMessage());
    assertInstanceOf(PersistenceException.class, e);
  }

  @Test
  void nativeOrdinalAndJdbcStyleThrows() {
    ParameterRecognitionException e = assertThrows(ParameterRecognitionException.class,
        () -> em.createNativeQuery(
            "select * from TrainService where fromStation = ?1 and toStation = ?", TrainService.class));
    assertEquals(NATIVE_MIX_MESSAGE, e.getMessage());
  }

  @Test
  @SuppressWarnings("unchecked")
  void nativeNamedAndOrdinalAreAccepted() {
    List<TrainService> trains = em.createNativeQuery(
            "select * from TrainService where fromStation = :from and toStation = ?1", TrainService.class)
        .setParameter("from", "Delhi")
        .setParameter(1, "Mumbai")
        .getResultList();
    assertEquals(List.of(101, 102), numbers(trains));
  }

  @Test
  @SuppressWarnings("unchecked")
  void nativeNamedOnly() {
    List<TrainService> trains = em.createNativeQuery(
            "select * from TrainService where fromStation = :from and toStation = :to", TrainService.class)
        .setParameter("from", "Delhi")
        .setParameter("to", "Mumbai")
        .getResultList();
    assertEquals(List.of(101, 102), numbers(trains));
  }

  @Test
  @SuppressWarnings("unchecked")
  void nativeJdbcStyleOnly() {
    List<TrainService> trains = em.createNativeQuery(
            "select * from TrainService where fromStation = ? and toStation = ?", TrainService.class)
        .setParameter(1, "Delhi")
        .setParameter(2, "Mumbai")
        .getResultList();
    assertEquals(List.of(101, 102), numbers(trains));
  }

  // ---------- 3. HQL / JPQL ----------

  @Test
  void hqlNamedAndOrdinalAreAcceptedByDefault() {
    List<TrainService> trains = em.createQuery(
            "from TrainService t where t.fromStation = :from and t.toStation = ?1", TrainService.class)
        .setParameter("from", "Delhi")
        .setParameter(1, "Mumbai")
        .getResultList();
    assertEquals(List.of(101, 102), numbers(trains));
  }

  @Test
  void hqlNamedAndOrdinalThrowWithStrictJpaCompliance() {
    try (EntityManagerFactory strict = Database.create(false, true);
         EntityManager strictEm = strict.createEntityManager()) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> strictEm.createQuery(
              "from TrainService t where t.fromStation = :from and t.toStation = ?1", TrainService.class));
      assertInstanceOf(StrictJpaComplianceViolation.class, e.getCause());
      assertEquals("Cannot mix ordinal and named parameters", e.getCause().getMessage());
    }
  }

  @Test
  void hqlOrdinalThenNamedThrowWithStrictJpaCompliance() {
    try (EntityManagerFactory strict = Database.create(false, true);
         EntityManager strictEm = strict.createEntityManager()) {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> strictEm.createQuery(
              "from TrainService t where t.fromStation = ?1 and t.toStation = :to", TrainService.class));
      assertInstanceOf(StrictJpaComplianceViolation.class, e.getCause());
      assertEquals("Cannot mix positional and named parameters", e.getCause().getMessage());
    }
  }

  @Test
  void hqlNamedOnlyWorksWithStrictJpaCompliance() {
    try (EntityManagerFactory strict = Database.create(false, true);
         EntityManager strictEm = strict.createEntityManager()) {
      List<TrainService> trains = strictEm.createQuery(
              "from TrainService t where t.fromStation = :from and t.toStation = :to", TrainService.class)
          .setParameter("from", "Delhi")
          .setParameter("to", "Mumbai")
          .getResultList();
      assertEquals(List.of(101, 102), numbers(trains));
    }
  }

  @Test
  void hqlJdbcStyleParameterThrows() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> em.createQuery("from TrainService t where t.fromStation = ?", TrainService.class));
    assertInstanceOf(ParameterLabelException.class, e.getCause());
    assertEquals("Unlabeled ordinal parameter ('?' rather than ?1)", e.getCause().getMessage());
  }

  @Test
  void hqlNamedAndJdbcStyleThrows() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> em.createQuery(
            "from TrainService t where t.fromStation = :from and t.toStation = ?", TrainService.class));
    assertEquals("Unlabeled ordinal parameter ('?' rather than ?1)", e.getCause().getMessage());
  }

  @Test
  void hqlOrdinalOnly() {
    List<TrainService> trains = em.createQuery(
            "from TrainService t where t.fromStation = ?1 and t.toStation = ?2", TrainService.class)
        .setParameter(1, "Delhi")
        .setParameter(2, "Mumbai")
        .getResultList();
    assertEquals(List.of(101, 102), numbers(trains));
  }

  @Test
  void hqlNamedParameterUsedTwice() {
    List<TrainService> trains = em.createQuery(
            "from TrainService t where t.fromStation = :city or t.toStation = :city order by t.trainNumber",
            TrainService.class)
        .setParameter("city", "Mumbai")
        .getResultList();
    assertEquals(List.of(101, 102, 301), numbers(trains));
  }

  // ---------- 4. Colons in native SQL ----------

  @Test
  void castRightAfterParameterBecomesPartOfTheName() {
    var query = em.createNativeQuery(
        "select trainNumber from TrainService where fromStation = :from::varchar");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> query.setParameter("from", "Delhi"));
    assertInstanceOf(UnknownParameterException.class, e.getCause());
    assertEquals("No parameter named ':from' in query with named parameters [from::varchar]", e.getMessage());
  }

  @Test
  void castFunctionFixesTheParameter() {
    List<?> numbers = em.createNativeQuery(
            "select trainNumber from TrainService where fromStation = cast(:from as varchar)")
        .setParameter("from", "Delhi")
        .getResultList();
    assertEquals(List.of(101, 102, 201), numbers);
  }

  @Test
  void escapedColonsFixTheParameter() {
    List<?> numbers = em.createNativeQuery(
            "select trainNumber from TrainService where fromStation = :from\\:\\:varchar")
        .setParameter("from", "Delhi")
        .getResultList();
    assertEquals(List.of(101, 102, 201), numbers);
  }

  @Test
  void castOnColumnAndColonInLiteralAreIgnored() {
    List<?> times = em.createNativeQuery(
            "select departsAt::varchar from TrainService where departsAt > '06:00:00' and fromStation = :from")
        .setParameter("from", "Delhi")
        .getResultList();
    assertEquals(List.of("16:30:00", "07:15:00"), times);
  }

  @Test
  void colonBeforeColumnNameIsReadAsParameter() {
    var query = em.createNativeQuery(
            "select cast(json_object('from':fromStation) as varchar) from TrainService where trainNumber = :number")
        .setParameter("number", 101);
    QueryParameterException e = assertThrows(QueryParameterException.class, query::getResultList);
    assertEquals("No argument for named parameter ':fromStation'", e.getMessage());
  }

  @Test
  void escapedColonIsSentAsColon() {
    List<?> json = em.createNativeQuery(
            "select cast(json_object('from'\\:fromStation) as varchar) from TrainService where trainNumber = :number")
        .setParameter("number", 101)
        .getResultList();
    assertEquals(List.of("{\"from\":\"Delhi\"}"), json);
  }

  @Test
  void spaceAfterColonAlsoWorks() {
    List<?> json = em.createNativeQuery(
            "select cast(json_object('from': fromStation) as varchar) from TrainService where trainNumber = :number")
        .setParameter("number", 101)
        .getResultList();
    assertEquals(List.of("{\"from\":\"Delhi\"}"), json);
  }

  // ---------- 5. Other parameter mistakes ----------

  @Test
  void unknownParameterNameThrows() {
    var query = em.createQuery("from TrainService t where t.fromStation = :from", TrainService.class);
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> query.setParameter("station", "Delhi"));
    assertEquals("No parameter named ':station' in query with named parameters [from]", e.getMessage());
  }

  @Test
  void positionZeroThrows() {
    var query = em.createQuery("from TrainService t where t.fromStation = ?1", TrainService.class);
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> query.setParameter(0, "Delhi"));
    assertEquals("No parameter labelled '?0' in query with ordinal parameters [1]", e.getMessage());
  }

  @Test
  void ordinalLabelZeroThrows() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> em.createQuery("from TrainService t where t.fromStation = ?0", TrainService.class));
    assertEquals("Ordinal parameter labels start from '?0' (ordinal parameters must be labelled from '?1')",
        e.getCause().getMessage());
  }

  @Test
  void missingArgumentThrows() {
    var query = em.createQuery("from TrainService t where t.fromStation = :from", TrainService.class);
    QueryParameterException e = assertThrows(QueryParameterException.class, query::getResultList);
    assertEquals("No argument for named parameter ':from'", e.getMessage());
  }
}
