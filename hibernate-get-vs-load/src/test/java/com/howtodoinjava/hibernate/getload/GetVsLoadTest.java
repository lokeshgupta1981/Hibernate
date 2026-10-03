package com.howtodoinjava.hibernate.getload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.LockMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.UnknownEntityTypeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("removal")
class GetVsLoadTest {

  private static final String SELECT =
      "select p1_0.id,p1_0.status,p1_0.trackingCode,p1_0.weightKg from Parcel p1_0 where p1_0.id=?";

  private SessionFactory sf;
  private Long id;

  @BeforeEach
  void setUp() {
    sf = Database.create(false);
    id = sf.fromTransaction(session -> {
      Parcel parcel = new Parcel("lisbon-1", "in transit", 2.5);
      session.persist(parcel);
      return parcel.getId();
    });
    SqlLog.clear();
  }

  @AfterEach
  void tearDown() {
    sf.close();
  }

  // ---- API status in Hibernate 7.4 ----

  @Test
  void loadWithClassAndIdNoLongerExists() {
    boolean present = Arrays.stream(Session.class.getMethods())
        .anyMatch(m -> m.getName().equals("load") && m.getParameterTypes()[0] == Class.class);
    assertFalse(present);
  }

  @Test
  void onlyLoadIntoAnInstanceIsLeft() {
    List<Method> loads = Arrays.stream(Session.class.getMethods())
        .filter(m -> m.getName().equals("load")).toList();
    assertEquals(1, loads.size());
    assertEquals(List.of(Object.class, Object.class), List.of(loads.get(0).getParameterTypes()));
  }

  @Test
  void getAndByIdAreDeprecatedForRemoval() throws Exception {
    Deprecated get = Session.class.getMethod("get", Class.class, Object.class)
        .getAnnotation(Deprecated.class);
    assertEquals("7.0", get.since());
    assertTrue(get.forRemoval());
    Deprecated byId = Session.class.getMethod("byId", Class.class).getAnnotation(Deprecated.class);
    assertEquals("7.1", byId.since());
    assertTrue(byId.forRemoval());
    assertNull(Session.class.getMethod("find", Class.class, Object.class)
        .getAnnotation(Deprecated.class));
    assertNull(Session.class.getMethod("getReference", Class.class, Object.class)
        .getAnnotation(Deprecated.class));
  }

  // ---- SQL timing ----

  @Test
  void getRunsSelectAtTheCall() {
    sf.inTransaction(session -> {
      Parcel parcel = session.get(Parcel.class, id);
      assertEquals(List.of(SELECT), SqlLog.statements());
      assertEquals(Parcel.class, parcel.getClass());
      assertEquals("in transit", parcel.getStatus());
    });
  }

  @Test
  void findRunsTheSameSelect() {
    sf.inTransaction(session -> {
      Parcel parcel = session.find(Parcel.class, id);
      assertEquals(List.of(SELECT), SqlLog.statements());
      assertEquals(Parcel.class, parcel.getClass());
      assertEquals("lisbon-1", parcel.getTrackingCode());
      assertEquals(2.5, parcel.getWeightKg());
    });
  }

  @Test
  void getReferenceRunsNoSqlAndReturnsProxy() {
    sf.inTransaction(session -> {
      Parcel parcel = session.getReference(Parcel.class, id);
      assertEquals("com.howtodoinjava.hibernate.getload.Parcel$HibernateProxy",
          parcel.getClass().getName());
      assertTrue(parcel instanceof Parcel);
      assertEquals(id, parcel.getId());
      assertFalse(Hibernate.isInitialized(parcel));
      assertEquals(List.of(), SqlLog.statements());
    });
  }

  @Test
  void firstGetterLoadsTheProxy() {
    sf.inTransaction(session -> {
      Parcel parcel = session.getReference(Parcel.class, id);
      assertEquals("in transit", parcel.getStatus());
      assertTrue(Hibernate.isInitialized(parcel));
      assertEquals(List.of(SELECT), SqlLog.statements());
    });
  }

  @Test
  void byIdLoadSelectsAndByIdGetReferenceDoesNot() {
    sf.inTransaction(session -> {
      Parcel loaded = session.byId(Parcel.class).load(id);
      assertEquals(Parcel.class, loaded.getClass());
      assertEquals(List.of(SELECT), SqlLog.statements());
    });
    SqlLog.clear();
    sf.inTransaction(session -> {
      Parcel ref = session.byId(Parcel.class).getReference(id);
      assertFalse(Hibernate.isInitialized(ref));
      assertEquals(List.of(), SqlLog.statements());
    });
  }

  @Test
  void entityNameVariants() {
    String entityName = "com.howtodoinjava.hibernate.getload.Parcel";
    sf.inTransaction(session -> assertEquals("lisbon-1",
        ((Parcel) session.find(entityName, id)).getTrackingCode()));
    sf.inTransaction(session ->
        assertFalse(Hibernate.isInitialized(session.getReference(entityName, id))));
  }

  @Test
  void simpleEntityNameIsNotAccepted() {
    sf.inSession(session -> {
      UnknownEntityTypeException e =
          assertThrows(UnknownEntityTypeException.class, () -> session.find("Parcel", id));
      assertEquals("Unknown entity type 'Parcel'", e.getMessage());
    });
  }

  @Test
  void lockModeOverloadsRunSelectForUpdate() {
    sf.inTransaction(session -> {
      Parcel parcel = session.get(Parcel.class, id, LockMode.PESSIMISTIC_WRITE);
      assertEquals(LockModeType.PESSIMISTIC_WRITE, session.getLockMode(parcel));
    });
    sf.inTransaction(session -> {
      Parcel parcel = session.find(Parcel.class, id, LockModeType.PESSIMISTIC_WRITE);
      assertEquals(LockModeType.PESSIMISTIC_WRITE, session.getLockMode(parcel));
    });
    assertEquals(List.of(SELECT + " for update", SELECT + " for update"), SqlLog.statements());
  }

  @Test
  void hibernateLockModeIsAFindOption() {
    sf.inTransaction(session -> {
      Parcel parcel = session.find(Parcel.class, id, LockMode.PESSIMISTIC_WRITE);
      assertEquals(LockModeType.PESSIMISTIC_WRITE, session.getLockMode(parcel));
    });
  }

  // ---- Missing row ----

  @Test
  void getFindAndByIdLoadReturnNull() {
    sf.inTransaction(session -> {
      assertNull(session.get(Parcel.class, 99L));
      assertNull(session.find(Parcel.class, 99L));
      assertNull(session.byId(Parcel.class).load(99L));
    });
    assertEquals(List.of(SELECT, SELECT, SELECT), SqlLog.statements());
  }

  @Test
  void getReferenceFailsOnFirstAccess() {
    sf.inTransaction(session -> {
      Parcel parcel = session.getReference(Parcel.class, 99L);
      assertEquals(99L, parcel.getId());
      assertEquals(List.of(), SqlLog.statements());
      EntityNotFoundException e = assertThrows(EntityNotFoundException.class, parcel::getStatus);
      assertEquals(EntityNotFoundException.class, e.getClass());
      assertFalse(ObjectNotFoundException.class.isInstance(e));
      assertEquals("No row with the given identifier exists for entity "
          + "[com.howtodoinjava.hibernate.getload.Parcel with id '99']", e.getMessage());
      assertEquals(List.of(SELECT), SqlLog.statements());
    });
  }

  @Test
  void byIdGetReferenceFailsOnFirstAccess() {
    sf.inTransaction(session -> {
      Parcel parcel = session.byId(Parcel.class).getReference(99L);
      assertThrows(EntityNotFoundException.class, parcel::getStatus);
    });
  }

  // ---- load(object, id) ----

  @Test
  void loadIntoInstanceFillsTheObject() {
    sf.inTransaction(session -> {
      Parcel parcel = new Parcel();
      session.load(parcel, id);
      assertEquals("lisbon-1", parcel.getTrackingCode());
      assertEquals("in transit", parcel.getStatus());
      assertEquals(List.of(SELECT), SqlLog.statements());
    });
  }

  @Test
  void loadIntoInstanceThrowsForMissingRow() {
    sf.inTransaction(session ->
        assertThrows(EntityNotFoundException.class, () -> session.load(new Parcel(), 99L)));
  }

  // ---- When to use which ----

  @Test
  void scanWithGetReferenceRunsOnlyInsert() {
    sf.inTransaction(session ->
        session.persist(new ScanEvent("Lisbon depot", session.getReference(Parcel.class, id))));
    assertEquals(List.of("insert into ScanEvent (location,parcel_id,id) values (?,?,default)"),
        SqlLog.statements());
    Long parcelId = sf.fromTransaction(session -> session
        .createSelectionQuery("select parcel.id from ScanEvent", Long.class).getSingleResult());
    assertEquals(id, parcelId);
  }

  @Test
  void scanWithFindRunsSelectAndInsert() {
    sf.inTransaction(session ->
        session.persist(new ScanEvent("Porto depot", session.find(Parcel.class, id))));
    assertEquals(List.of(SELECT,
        "insert into ScanEvent (location,parcel_id,id) values (?,?,default)"), SqlLog.statements());
  }

  @Test
  void secondLookupInSameSessionRunsNoSql() {
    sf.inTransaction(session -> {
      Parcel first = session.find(Parcel.class, id);
      Parcel second = session.get(Parcel.class, id);
      assertSame(first, second);
    });
    assertEquals(List.of(SELECT), SqlLog.statements());
  }

  @Test
  void findAfterGetReferenceReturnsTheProxy() {
    sf.inTransaction(session -> {
      Parcel ref = session.getReference(Parcel.class, id);
      Parcel found = session.find(Parcel.class, id);
      assertSame(ref, found);
      assertTrue(Hibernate.isInitialized(found));
    });
    assertEquals(List.of(SELECT), SqlLog.statements());
  }

  @Test
  void proxyCannotLoadAfterSessionCloses() {
    Parcel detached = sf.fromTransaction(session -> session.getReference(Parcel.class, id));
    LazyInitializationException e =
        assertThrows(LazyInitializationException.class, detached::getStatus);
    assertEquals("Could not initialize proxy [com.howtodoinjava.hibernate.getload.Parcel#1]"
        + " - no session", e.getMessage());
  }

  @Test
  void foundEntityWorksAfterSessionCloses() {
    Parcel detached = sf.fromTransaction(session -> session.find(Parcel.class, id));
    assertEquals("in transit", detached.getStatus());
  }
}
