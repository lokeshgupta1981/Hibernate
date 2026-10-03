package com.howtodoinjava.hibernate.interceptors;

import java.time.LocalDateTime;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class InterceptorDemo {

  static final LocalDateTime NINE = LocalDateTime.of(2026, 11, 12, 9, 0);
  static final LocalDateTime ELEVEN = LocalDateTime.of(2026, 11, 12, 11, 0);

  public static void main(String[] args) {
    try (SessionFactory sf = Database.create(true)) {

      step("1. Book an appointment (onPersist)");
      Long id;
      try (Session session = AuditInterceptor.openSession(sf)) {
        session.beginTransaction();
        Appointment lokesh = new Appointment("Lokesh", "Dr. Mehta", NINE);
        session.persist(lokesh);
        System.out.println("after persist(): status = " + lokesh.getStatus());
        session.getTransaction().commit();
        id = lokesh.getId();
      }

      step("2. Move the slot and change the doctor (onFlushDirty)");
      try (Session session = AuditInterceptor.openSession(sf)) {
        session.beginTransaction();
        Appointment a = session.find(Appointment.class, id);
        a.setSlot(ELEVEN);
        a.setDoctor("Dr. Rao");
        session.getTransaction().commit();
      }

      step("3. Cancel and roll back");
      try (Session session = AuditInterceptor.openSession(sf)) {
        session.beginTransaction();
        Appointment a = session.find(Appointment.class, id);
        a.setStatus("cancelled");
        session.flush();
        session.getTransaction().rollback();
      }

      step("4. Delete the appointment (onRemove)");
      try (Session session = AuditInterceptor.openSession(sf)) {
        session.beginTransaction();
        session.remove(session.find(Appointment.class, id));
        session.getTransaction().commit();
      }

      step("Audit trail");
      sf.inTransaction(s -> s.createSelectionQuery("from AuditLog order by id", AuditLog.class)
          .getResultList().forEach(System.out::println));
    }

    step("5. Callback order");
    try (SessionFactory sf = Database.create(true)) {
      TracingInterceptor trace = new TracingInterceptor();
      Long id;
      try (Session session = sf.withOptions().interceptor(trace).openSession()) {
        session.beginTransaction();
        Appointment a = new Appointment("Lokesh", "Dr. Mehta", NINE);
        System.out.println("persist()");
        session.persist(a);
        System.out.println("commit()");
        session.getTransaction().commit();
        id = a.getId();
      }
      try (Session session = sf.withOptions().interceptor(trace).openSession()) {
        session.beginTransaction();
        System.out.println("find()");
        Appointment a = session.find(Appointment.class, id);
        a.setStatus("confirmed");
        System.out.println("commit()");
        session.getTransaction().commit();
      }
    }

    step("6. SessionFactory-scoped interceptor");
    LoggingInterceptor logging = new LoggingInterceptor();
    try (SessionFactory sf = Database.withSharedInterceptor(logging, false)) {
      Long id = sf.fromTransaction(s -> {
        Appointment a = new Appointment("Anna", "Dr. Mehta", NINE);
        s.persist(a);
        return a.getId();
      });
      sf.inTransaction(s -> s.find(Appointment.class, id).setStatus("confirmed"));
    }

    step("7. StatementInspector");
    SqlTagInspector inspector = new SqlTagInspector();
    try (SessionFactory sf = Database.withStatementInspector(inspector, false)) {
      sf.inTransaction(s -> s.persist(new Appointment("Lokesh", "Dr. Mehta", NINE)));
      sf.inTransaction(s -> s.createSelectionQuery("from Appointment where doctor = :d", Appointment.class)
          .setParameter("d", "Dr. Mehta").getResultList());
      inspector.statements().forEach(System.out::println);
    }
  }

  static void step(String title) {
    System.out.println();
    System.out.println("--- " + title + " ---");
  }
}
