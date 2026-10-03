package com.howtodoinjava.hibernate.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InterceptorTest {

  static final LocalDateTime NINE = LocalDateTime.of(2026, 11, 12, 9, 0);
  static final LocalDateTime ELEVEN = LocalDateTime.of(2026, 11, 12, 11, 0);

  private SessionFactory sf;

  @AfterEach
  void close() {
    if (sf != null) {
      sf.close();
    }
  }

  private Long book(String patient) {
    try (Session session = AuditInterceptor.openSession(sf)) {
      session.beginTransaction();
      Appointment a = new Appointment(patient, "Dr. Mehta", NINE);
      session.persist(a);
      session.getTransaction().commit();
      return a.getId();
    }
  }

  private List<String> auditTrail() {
    return sf.fromTransaction(s -> s.createSelectionQuery("from AuditLog order by id", AuditLog.class)
        .getResultList().stream().map(AuditLog::toString).toList());
  }

  // --- onPersist ---

  @Test
  void onPersistRunsAtPersistAndFillsDefaultStatus() {
    sf = Database.create(false);
    try (Session session = AuditInterceptor.openSession(sf)) {
      session.beginTransaction();
      Appointment a = new Appointment("Lokesh", "Dr. Mehta", NINE);
      session.persist(a);
      assertEquals("booked", a.getStatus());          // before any flush
      session.getTransaction().commit();
    }
    String status = sf.fromTransaction(s -> s.createSelectionQuery(
        "select status from Appointment", String.class).getSingleResult());
    assertEquals("booked", status);
    assertEquals(List.of("insert Appointment#1"), auditTrail());
  }

  // --- onFlushDirty ---

  @Test
  void onFlushDirtyRecordsOnlyChangedPropertiesWithOldAndNewValues() {
    sf = Database.create(false);
    Long id = book("Lokesh");
    try (Session session = AuditInterceptor.openSession(sf)) {
      session.beginTransaction();
      Appointment a = session.find(Appointment.class, id);
      a.setSlot(ELEVEN);
      a.setDoctor("Dr. Rao");
      session.getTransaction().commit();
    }
    assertEquals(List.of(
        "insert Appointment#1",
        "update Appointment#1 doctor: Dr. Mehta -> Dr. Rao",
        "update Appointment#1 slot: 2026-11-12T09:00 -> 2026-11-12T11:00"), auditTrail());
  }

  @Test
  void propertyNamesAreAlphabetical() {
    sf = Database.create(false);
    List<String> names = new ArrayList<>();
    Interceptor capture = new Interceptor() {
      @Override
      public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        names.addAll(List.of(propertyNames));
        return false;
      }
    };
    try (Session session = sf.withOptions().interceptor(capture).openSession()) {
      session.beginTransaction();
      session.persist(new Appointment("Lokesh", "Dr. Mehta", NINE));
      session.getTransaction().commit();
    }
    assertEquals(List.of("doctor", "patientName", "slot", "status"), names);
  }

  @Test
  void onFlushDirtyIsNotCalledWhenNothingChanged() {
    sf = Database.create(false);
    Long id = book("Lokesh");
    TracingInterceptor trace = new TracingInterceptor();
    try (Session session = sf.withOptions().interceptor(trace).openSession()) {
      session.beginTransaction();
      session.find(Appointment.class, id);
      session.getTransaction().commit();
    }
    assertTrue(trace.calls().contains("findDirty"));
    assertFalse(trace.calls().contains("onFlushDirty"));
  }

  @Test
  void rollbackAlsoRemovesTheAuditRow() {
    sf = Database.create(false);
    Long id = book("Lokesh");
    try (Session session = AuditInterceptor.openSession(sf)) {
      session.beginTransaction();
      session.find(Appointment.class, id).setStatus("cancelled");
      session.flush();                                  // UPDATE and audit INSERT sent
      session.getTransaction().rollback();
    }
    assertEquals(List.of("insert Appointment#1"), auditTrail());
  }

  // --- onRemove ---

  @Test
  void onRemoveRecordsTheDelete() {
    sf = Database.create(false);
    Long id = book("Lokesh");
    try (Session session = AuditInterceptor.openSession(sf)) {
      session.beginTransaction();
      session.remove(session.find(Appointment.class, id));
      session.getTransaction().commit();
    }
    assertEquals(List.of("insert Appointment#1", "delete Appointment#1"), auditTrail());
  }

  @Test
  void bulkJpqlUpdateSkipsTheInterceptor() {
    sf = Database.create(false);
    book("Lokesh");
    book("Anna");
    int updated;
    try (Session session = AuditInterceptor.openSession(sf)) {
      session.beginTransaction();
      updated = session.createMutationQuery("update Appointment set status = 'confirmed'").executeUpdate();
      session.getTransaction().commit();
    }
    assertEquals(2, updated);
    assertEquals(List.of("insert Appointment#1", "insert Appointment#2"), auditTrail());
  }

  // --- callback order ---

  @Test
  void callbackOrder() {
    sf = Database.create(false);
    TracingInterceptor trace = new TracingInterceptor();
    Long id;
    try (Session session = sf.withOptions().interceptor(trace).openSession()) {
      session.beginTransaction();
      Appointment a = new Appointment("Lokesh", "Dr. Mehta", NINE);
      session.persist(a);
      session.getTransaction().commit();
      id = a.getId();
    }
    assertEquals(List.of("afterTransactionBegin", "onPersist", "preFlush", "findDirty", "postFlush",
        "beforeTransactionCompletion", "afterTransactionCompletion"), trace.calls());

    trace.calls().clear();
    try (Session session = sf.withOptions().interceptor(trace).openSession()) {
      session.beginTransaction();
      session.find(Appointment.class, id).setStatus("confirmed");
      session.getTransaction().commit();
    }
    assertEquals(List.of("afterTransactionBegin", "onLoad", "preFlush", "findDirty", "onFlushDirty",
        "postFlush", "beforeTransactionCompletion", "afterTransactionCompletion"), trace.calls());
  }

  @Test
  void onRemoveRunsAtRemoveBeforeTheFlush() {
    sf = Database.create(false);
    Long id = book("Lokesh");
    TracingInterceptor trace = new TracingInterceptor();
    try (Session session = sf.withOptions().interceptor(trace).openSession()) {
      session.beginTransaction();
      session.remove(session.find(Appointment.class, id));
      assertEquals(List.of("afterTransactionBegin", "onLoad", "onRemove"), trace.calls());
      session.getTransaction().commit();
    }
    assertEquals(0L, (long) sf.fromTransaction(s -> s.createSelectionQuery(
        "select count(*) from Appointment", Long.class).getSingleResult()));
  }

  // --- changing state ---

  @Test
  void changingTheEntityInOnFlushDirtyIsNotSaved() {
    sf = Database.create(false);
    Long id = book("Lokesh");
    Interceptor wrong = new Interceptor() {
      @Override
      public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
                                  String[] propertyNames, Type[] types) {
        ((Appointment) entity).setDoctor("Dr. Rao");     // wrong: change the entity
        return false;
      }
    };
    try (Session session = sf.withOptions().interceptor(wrong).openSession()) {
      session.beginTransaction();
      session.find(Appointment.class, id).setStatus("confirmed");
      session.getTransaction().commit();
    }
    assertEquals("Dr. Mehta", doctorOf(id));
  }

  @Test
  void changingTheStateArrayInOnFlushDirtyIsSaved() {
    sf = Database.create(false);
    Long id = book("Lokesh");
    Interceptor right = new Interceptor() {
      @Override
      public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
                                  String[] propertyNames, Type[] types) {
        currentState[0] = "Dr. Rao";                       // right: change the state array
        return true;
      }
    };
    try (Session session = sf.withOptions().interceptor(right).openSession()) {
      session.beginTransaction();
      Appointment a = session.find(Appointment.class, id);
      a.setStatus("confirmed");
      session.getTransaction().commit();
      assertEquals("Dr. Rao", a.getDoctor());            // copied back to the entity
    }
    assertEquals("Dr. Rao", doctorOf(id));
  }

  private String doctorOf(Long id) {
    return sf.fromTransaction(s -> s.find(Appointment.class, id).getDoctor());
  }

  // --- scopes ---

  public static class CountingInterceptor implements Interceptor {
    static final AtomicInteger INSTANCES = new AtomicInteger();

    public CountingInterceptor() {
      INSTANCES.incrementAndGet();
    }
  }

  @Test
  void sessionScopedSettingCreatesOneInstancePerSession() {
    sf = Database.withSessionScopedInterceptor(CountingInterceptor.class, false);
    int before = CountingInterceptor.INSTANCES.get();
    try (Session s1 = sf.openSession(); Session s2 = sf.openSession()) {
      assertNotSame(s1, s2);
    }
    assertEquals(before + 2, CountingInterceptor.INSTANCES.get());
  }

  @Test
  void sharedInterceptorSeesEverySession() {
    LoggingInterceptor logging = new LoggingInterceptor();
    sf = Database.withSharedInterceptor(logging, false);
    Long id = sf.fromTransaction(s -> {
      Appointment a = new Appointment("Anna", "Dr. Mehta", NINE);
      s.persist(a);
      return a.getId();
    });
    sf.inTransaction(s -> s.find(Appointment.class, id).setStatus("confirmed"));
    sf.inTransaction(s -> s.find(Appointment.class, id).setStatus("cancelled"));
    assertEquals(List.of(
        "Appointment#1 changed from [Dr. Mehta, Anna, 2026-11-12T09:00, null] to [Dr. Mehta, Anna, 2026-11-12T09:00, confirmed]",
        "Appointment#1 changed from [Dr. Mehta, Anna, 2026-11-12T09:00, confirmed] to [Dr. Mehta, Anna, 2026-11-12T09:00, cancelled]"),
        logging.log());
  }

  @Test
  void sessionInterceptorReplacesTheSharedOne() {
    LoggingInterceptor logging = new LoggingInterceptor();
    sf = Database.withSharedInterceptor(logging, false);
    Long id = sf.fromTransaction(s -> {
      Appointment a = new Appointment("Anna", "Dr. Mehta", NINE);
      s.persist(a);
      return a.getId();
    });
    TracingInterceptor trace = new TracingInterceptor();
    try (Session session = sf.withOptions().interceptor(trace).openSession()) {
      session.beginTransaction();
      session.find(Appointment.class, id).setStatus("confirmed");
      session.getTransaction().commit();
    }
    assertTrue(logging.log().isEmpty());
    assertTrue(trace.calls().contains("onFlushDirty"));
  }

  // --- StatementInspector ---

  @Test
  void statementInspectorRewritesEverySqlStatement() {
    SqlTagInspector inspector = new SqlTagInspector();
    sf = Database.withStatementInspector(inspector, false);
    sf.inTransaction(s -> s.persist(new Appointment("Lokesh", "Dr. Mehta", NINE)));
    List<Appointment> found = sf.fromTransaction(s -> s.createSelectionQuery(
        "from Appointment where doctor = :d", Appointment.class).setParameter("d", "Dr. Mehta").getResultList());
    assertEquals(1, found.size());
    assertEquals(List.of(
        "/* clinic-app */ select next value for Appointment_SEQ",
        "/* clinic-app */ insert into Appointment (doctor,patientName,slot,status,id) values (?,?,?,?,?)",
        "/* clinic-app */ select a1_0.id,a1_0.doctor,a1_0.patientName,a1_0.slot,a1_0.status from Appointment a1_0 where a1_0.doctor=?"),
        inspector.statements());
  }

  @Test
  void sessionLevelStatementInspector() {
    sf = Database.create(false);
    List<String> seen = new ArrayList<>();
    UnaryOperator<String> collect = sql -> {
      seen.add(sql);
      return sql;
    };
    try (Session session = sf.withOptions().statementInspector(collect).openSession()) {
      session.createSelectionQuery("from Appointment", Appointment.class).getResultList();
    }
    assertEquals(List.of("select a1_0.id,a1_0.doctor,a1_0.patientName,a1_0.slot,a1_0.status from Appointment a1_0"), seen);
  }

  // --- API facts ---

  @Test
  void emptyInterceptorIsGoneFromThePublicApi() {
    assertThrows(ClassNotFoundException.class, () -> Class.forName("org.hibernate.EmptyInterceptor"));
  }

  @Test
  void onSaveAndOldOnDeleteAreDeprecated() throws Exception {
    Method onSave = Interceptor.class.getMethod("onSave",
        Object.class, Object.class, Object[].class, String[].class, Type[].class);
    Method onDelete = Interceptor.class.getMethod("onDelete",
        Object.class, Object.class, Object[].class, String[].class, Type[].class);
    assertEquals("6.6", onSave.getAnnotation(Deprecated.class).since());
    assertEquals("6.6", onDelete.getAnnotation(Deprecated.class).since());
    Method onPersist = Interceptor.class.getMethod("onPersist",
        Object.class, Object.class, Object[].class, String[].class, Type[].class);
    assertNull(onPersist.getAnnotation(Deprecated.class));
    assertTrue(onPersist.isDefault());
  }
}
