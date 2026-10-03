package com.howtodoinjava.hibernate.getload;

import jakarta.persistence.LockModeType;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;

/**
 * Session.load(Class, id) was removed in Hibernate 7.0 and Session.get() is deprecated.
 * This demo runs the deprecated calls next to their replacements and prints the SQL of each.
 */
@SuppressWarnings("removal")
public class GetVsLoadDemo {

  public static void main(String[] args) {
    try (SessionFactory sf = Database.create(true)) {

      step("0. Save a parcel");
      Long id = sf.fromTransaction(session -> {
        Parcel parcel = new Parcel("lisbon-1", "in transit", 2.5);
        session.persist(parcel);
        return parcel.getId();
      });
      System.out.println("id=" + id);

      step("1. Old: session.get() (deprecated in 7.0)");
      sf.inTransaction(session -> {
        Parcel parcel = session.get(Parcel.class, id);
        System.out.println("after get(): class=" + parcel.getClass().getSimpleName()
            + " " + parcel);
      });

      step("2. New: session.find()");
      sf.inTransaction(session -> {
        Parcel parcel = session.find(Parcel.class, id);
        System.out.println("after find(): class=" + parcel.getClass().getSimpleName()
            + " " + parcel);
      });

      step("3. New: session.getReference() (replaces load())");
      sf.inTransaction(session -> {
        Parcel parcel = session.getReference(Parcel.class, id);
        System.out.println("after getReference(): class=" + parcel.getClass().getName());
        System.out.println("id=" + parcel.getId() + " initialized=" + Hibernate.isInitialized(parcel));
        System.out.println("calling getStatus()");
        System.out.println("status=" + parcel.getStatus()
            + " initialized=" + Hibernate.isInitialized(parcel));
      });

      step("4. Old: byId().load() and byId().getReference() (deprecated in 7.1)");
      sf.inTransaction(session -> {
        Parcel loaded = session.byId(Parcel.class).load(id);
        System.out.println("byId().load(): class=" + loaded.getClass().getSimpleName());
      });
      sf.inTransaction(session -> {
        Parcel ref = session.byId(Parcel.class).getReference(id);
        System.out.println("byId().getReference(): initialized=" + Hibernate.isInitialized(ref));
      });

      step("5. Missing row");
      sf.inTransaction(session -> System.out.println("get(99)=" + session.get(Parcel.class, 99L)));
      sf.inTransaction(session -> System.out.println("find(99)=" + session.find(Parcel.class, 99L)));
      sf.inTransaction(session ->
          System.out.println("byId().load(99)=" + session.byId(Parcel.class).load(99L)));
      try {
        sf.inTransaction(session -> {
          Parcel parcel = session.getReference(Parcel.class, 99L);
          System.out.println("getReference(99) returned, id=" + parcel.getId());
          parcel.getStatus();
        });
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("6. session.load(object, id), the one load() left");
      sf.inTransaction(session -> {
        Parcel parcel = new Parcel();
        session.load(parcel, id);
        System.out.println("filled: " + parcel);
      });
      try {
        sf.inTransaction(session -> session.load(new Parcel(), 99L));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("7. Entity name instead of class");
      String entityName = "com.howtodoinjava.hibernate.getload.Parcel";
      sf.inTransaction(session -> {
        Object parcel = session.find(entityName, id);
        System.out.println("find(entityName, id): " + parcel);
      });
      sf.inTransaction(session -> {
        Object ref = session.getReference(entityName, id);
        System.out.println("getReference(entityName, id): initialized=" + Hibernate.isInitialized(ref));
      });

      step("8. Lock mode: get() vs find()");
      sf.inTransaction(session -> session.get(Parcel.class, id, LockMode.PESSIMISTIC_WRITE));
      sf.inTransaction(session -> session.find(Parcel.class, id, LockModeType.PESSIMISTIC_WRITE));

      step("9. Record a scan with getReference(): no SELECT for the parcel");
      sf.inTransaction(session ->
          session.persist(new ScanEvent("Lisbon depot", session.getReference(Parcel.class, id))));
      System.out.println("same scan with find():");
      sf.inTransaction(session ->
          session.persist(new ScanEvent("Porto depot", session.find(Parcel.class, id))));

      step("10. Same id twice in one session");
      sf.inTransaction(session -> {
        Parcel first = session.find(Parcel.class, id);
        Parcel second = session.get(Parcel.class, id);
        System.out.println("get() after find(), no SQL, same object: " + (first == second));
      });
      sf.inTransaction(session -> {
        Parcel ref = session.getReference(Parcel.class, id);
        Parcel found = session.find(Parcel.class, id);
        System.out.println("find() returns the proxy: " + (ref == found)
            + " initialized=" + Hibernate.isInitialized(found));
        Parcel again = session.find(Parcel.class, id);
        System.out.println("second find(), no SQL, same object: " + (again == found));
      });

      step("11. Read a reference after the session is closed");
      Parcel detached = sf.fromTransaction(session -> session.getReference(Parcel.class, id));
      try {
        detached.getStatus();
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
