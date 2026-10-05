package com.howtodoinjava.hibernate.namedquery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.hibernate.DuplicateMappingException;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.query.NamedQueryValidationException;
import org.hibernate.query.QueryTypeMismatchException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NamedQueryTest {

  private EntityManagerFactory emf;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    Database.seed(emf);
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private static List<Integer> numbers(List<Room> rooms) {
    return rooms.stream().map(Room::getNumber).toList();
  }

  @Test
  void namedParameter() {
    List<Room> doubles = emf.callInTransaction(em -> em
        .createNamedQuery("Room.findAvailableByType", Room.class)
        .setParameter("type", "double")
        .getResultList());
    assertEquals(List.of(201, 202), numbers(doubles));
  }

  @Test
  void joinFetchLoadsTheRoom() {
    Booking booking = emf.callInTransaction(em -> em
        .createNamedQuery("Booking.findByGuest", Booking.class)
        .setParameter("guest", "Lokesh")
        .getSingleResult());
    assertTrue(Hibernate.isInitialized(booking.getRoom()));
    assertEquals(102, booking.getRoom().getNumber());
  }

  @Test
  void positionalParameters() {
    List<Room> rooms = emf.callInTransaction(em -> em
        .createNamedQuery("Room.findByRateBetween", Room.class)
        .setParameter(1, new BigDecimal("100"))
        .setParameter(2, new BigDecimal("300"))
        .getResultList());
    assertEquals(List.of(201, 202, 301), numbers(rooms));
  }

  @Test
  void collectionParameter() {
    List<Room> rooms = emf.callInTransaction(em -> em
        .createNamedQuery("Room.findByTypes", Room.class)
        .setParameter("types", List.of("single", "suite"))
        .getResultList());
    assertEquals(List.of(101, 102, 301), numbers(rooms));
  }

  @Test
  void scalarResult() {
    Long free = emf.callInTransaction(em -> em
        .createNamedQuery("Room.countAvailable", Long.class)
        .getSingleResult());
    assertEquals(3L, free);
  }

  @Test
  void objectArrayAndRecordResults() {
    List<Object[]> rows = emf.callInTransaction(em -> em
        .createNamedQuery("Room.findRates", Object[].class)
        .setParameter("type", "single")
        .getResultList());
    assertEquals(101, rows.get(0)[0]);
    assertEquals(new BigDecimal("80.00"), rows.get(0)[1]);

    List<RoomRate> rates = emf.callInTransaction(em -> em
        .createNamedQuery("Room.findRates", RoomRate.class)
        .setParameter("type", "single")
        .getResultList());
    assertEquals(List.of(new RoomRate(101, new BigDecimal("80.00")),
        new RoomRate(102, new BigDecimal("80.00"))), rates);
  }

  @Test
  void hibernateReadOnlyQueryIsNotDirtyChecked() {
    emf.runInTransaction(em -> {
      List<Booking> longStays = em.createNamedQuery("Booking.findLongStays", Booking.class)
          .setParameter("nights", 3)
          .getResultList();
      assertEquals(List.of("Alex", "Lokesh"), longStays.stream().map(Booking::getGuest).toList());
      assertTrue(em.unwrap(Session.class).isReadOnly(longStays.get(0)));
      longStays.get(0).setNights(10);
    });
    Integer nights = emf.callInTransaction(em -> em
        .createQuery("select nights from Booking where guest = 'Alex'", Integer.class)
        .getSingleResult());
    assertEquals(5, nights);
  }

  @Test
  void nativeQueryMappedToEntity() {
    List<Room> rooms = emf.callInTransaction(em -> em
        .createNamedQuery("Room.findFreeUnderRate", Room.class)
        .setParameter(1, new BigDecimal("100"))
        .getResultList());
    assertEquals(List.of(101), numbers(rooms));
  }

  @Test
  void nativeQueryMappedToRecord() {
    List<RoomCount> counts = emf.callInTransaction(em -> em
        .createNamedQuery("Room.countByType", RoomCount.class)
        .getResultList());
    assertEquals(List.of(new RoomCount("double", 2L), new RoomCount("single", 2L),
        new RoomCount("suite", 1L)), counts);
  }

  @Test
  void queriesFromOrmXml() {
    Long count = emf.callInTransaction(em -> em
        .createNamedQuery("Booking.countByRoom", Long.class)
        .setParameter("number", 301)
        .getSingleResult());
    assertEquals(1L, count);
    List<?> guests = emf.callInTransaction(em -> em
        .createNamedQuery("Booking.guestsSql")
        .getResultList());
    assertEquals(List.of("Alex", "Lokesh", "Maria"), guests);
  }

  @Test
  void ormXmlReplacesAnnotationWithSameName() {
    try (EntityManagerFactory overridden = Database.configuration(false)
        .mappingFile("META-INF/override-orm.xml")
        .managedClasses(Room.class, Booking.class)
        .createEntityManagerFactory()) {
      Database.seed(overridden);
      List<Room> rooms = overridden.callInTransaction(em -> em
          .createNamedQuery("Room.findByTypes", Room.class)
          .setParameter("types", List.of("single", "suite"))
          .getResultList());
      assertEquals(List.of(101), numbers(rooms));
    }
  }

  @Test
  void typedQueryReferenceFromMetamodel() {
    List<Room> singles = emf.callInTransaction(em -> em
        .createQuery(Room_._Room_findAvailableByType_)
        .setParameter("type", "single")
        .getResultList());
    assertEquals(List.of(101), numbers(singles));
    Long free = emf.callInTransaction(em -> em.createQuery(Room_._Room_countAvailable_).getSingleResult());
    assertEquals(3L, free);
  }

  @Test
  void factoryListsQueriesWithResultClass() {
    assertEquals(Set.of("Room.findAvailableByType", "Room.findFreeUnderRate"),
        emf.getNamedQueries(Room.class).keySet());
  }

  @Test
  void generatedHqlQueryMethods() {
    List<Room> doubles = emf.callInTransaction(em -> HotelQueries_.findAvailable(em, "double"));
    assertEquals(List.of(201, 202), numbers(doubles));
    long bookings = emf.callInTransaction(em -> HotelQueries_.countBookings(em, 102));
    assertEquals(1L, bookings);
  }

  @Test
  void updateQuery() {
    int updated = emf.callInTransaction(em -> em.createNamedQuery("Room.updateRate")
        .setParameter("rate", new BigDecimal("90.00"))
        .setParameter("type", "single")
        .executeUpdate());
    assertEquals(2, updated);
    List<RoomRate> rates = emf.callInTransaction(em -> em
        .createNamedQuery("Room.findRates", RoomRate.class)
        .setParameter("type", "single")
        .getResultList());
    assertEquals(new BigDecimal("90.00"), rates.get(0).nightlyRate());
  }

  @Test
  void sessionApi() {
    emf.runInTransaction(em -> {
      Session session = em.unwrap(Session.class);
      List<Room> rooms = session.createNamedSelectionQuery("Room.findAvailableByType", Room.class)
          .setParameter("type", "double")
          .getResultList();
      assertEquals(List.of(201, 202), numbers(rooms));
      int updated = session.createNamedMutationQuery("Room.updateRate")
          .setParameter("rate", new BigDecimal("130.00"))
          .setParameter("type", "double")
          .executeUpdate();
      assertEquals(2, updated);
    });
  }

  @Test
  void unknownNameFailsWhenCreated() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> emf.runInTransaction(em -> em.createNamedQuery("Room.findCheap", Room.class)));
    assertEquals("No query named 'Room.findCheap'", e.getMessage());
  }

  @Test
  void wrongResultClassFails() {
    QueryTypeMismatchException e = assertThrows(QueryTypeMismatchException.class,
        () -> emf.runInTransaction(em -> em.createNamedQuery("Room.countAvailable", Room.class)));
    assertTrue(e.getMessage().startsWith("Incorrect query result type: query produces 'java.lang.Long'"));
  }

  @Test
  void nativeQueryWithoutResultClassRejectsTypedCall() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> emf.runInTransaction(em -> em.createNamedQuery("Booking.guestsSql", String.class)));
    assertEquals("Named query exists, but did not specify a resultClass", e.getMessage());
  }

  @Test
  void typoFailsAtStartup() {
    PersistenceException e = assertThrows(PersistenceException.class,
        () -> Database.create(false, BrokenQueries.class).close());
    assertInstanceOf(NamedQueryValidationException.class, e.getCause());
    assertTrue(e.getCause().getMessage().contains(
        "Error in query named 'Room.findFree': Could not resolve attribute 'availble'"));
  }

  @Test
  void typoFailsLaterWhenStartupCheckIsOff() {
    try (EntityManagerFactory lenient = Database.configuration(false)
        .managedClasses(Room.class, Booking.class, BrokenQueries.class)
        .property("hibernate.query.startup_check", false)
        .createEntityManagerFactory()) {
      assertTrue(lenient.isOpen());
      assertThrows(IllegalArgumentException.class,
          () -> lenient.runInTransaction(em -> em.createNamedQuery("Room.findFree", Room.class)));
    }
  }

  @Test
  void nativeTypoIsNotCheckedAtStartup() {
    try (EntityManagerFactory factory = Database.create(false, UncheckedNativeQueries.class)) {
      assertTrue(factory.isOpen());
      assertThrows(SQLGrammarException.class, () -> factory.runInTransaction(em -> em
          .createNamedQuery("Room.findFreeSql", Room.class)
          .getResultList()));
    }
  }

  @Test
  void duplicateNameFailsAtStartup() {
    DuplicateMappingException e = assertThrows(DuplicateMappingException.class,
        () -> Database.create(false, DuplicateQueries.class).close());
    assertEquals("Duplicate named query 'Room.countAvailable'", e.getMessage());
  }

  @Test
  void wrongEntityNameFailsAtStartup() {
    PersistenceException e = assertThrows(PersistenceException.class,
        () -> Database.create(false, BadEntityNameQueries.class).close());
    assertTrue(e.getCause().getMessage().contains(
        "Error in query named 'Room.findAllRooms': Could not resolve root entity 'Rooms'"));
  }

  @Test
  void wrongTypeFailsAtStartup() {
    PersistenceException e = assertThrows(PersistenceException.class,
        () -> Database.create(false, BadTypeQueries.class).close());
    assertTrue(e.getCause().getMessage().contains("Cannot compare left expression of type "
        + "'java.math.BigDecimal' with right expression of type 'java.lang.String'"));
  }

  @Test
  void nativeQueryWithResultClassInOrmXml() {
    List<String> guests = emf.callInTransaction(em -> em
        .createNamedQuery("Booking.guestNames", String.class)
        .getResultList());
    assertEquals(List.of("Alex", "Lokesh", "Maria"), guests);
  }

  @Test
  void bulkUpdateDoesNotChangeLoadedEntities() {
    emf.runInTransaction(em -> {
      Room room = em.createNamedQuery("Room.findAvailableByType", Room.class)
          .setParameter("type", "single")
          .getSingleResult();
      em.createNamedQuery("Room.updateRate")
          .setParameter("rate", new BigDecimal("90.00"))
          .setParameter("type", "single")
          .executeUpdate();
      assertEquals(new BigDecimal("80.00"), room.getNightlyRate());
      em.refresh(room);
      assertEquals(new BigDecimal("90.00"), room.getNightlyRate());
    });
  }
}
