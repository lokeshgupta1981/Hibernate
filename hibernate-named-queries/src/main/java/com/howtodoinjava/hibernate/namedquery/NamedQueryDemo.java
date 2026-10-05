package com.howtodoinjava.hibernate.namedquery;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import org.hibernate.Session;

public class NamedQueryDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Save five rooms and three bookings");
      Database.seed(emf);

      step("2. Run a @NamedQuery with a named parameter");
      emf.runInTransaction(em -> {
        List<Room> doubles = em.createNamedQuery("Room.findAvailableByType", Room.class)
            .setParameter("type", "double")
            .getResultList();
        print("doubles = " + doubles);
      });

      step("3. join fetch in a named query");
      emf.runInTransaction(em -> {
        Booking booking = em.createNamedQuery("Booking.findByGuest", Booking.class)
            .setParameter("guest", "Lokesh")
            .getSingleResult();
        print("booking = " + booking);
      });

      step("4. Positional parameters");
      emf.runInTransaction(em -> {
        List<Room> rooms = em.createNamedQuery("Room.findByRateBetween", Room.class)
            .setParameter(1, new BigDecimal("100"))
            .setParameter(2, new BigDecimal("300"))
            .getResultList();
        print("rooms = " + rooms);
      });

      step("5. Collection parameter");
      emf.runInTransaction(em -> {
        List<Room> rooms = em.createNamedQuery("Room.findByTypes", Room.class)
            .setParameter("types", List.of("single", "suite"))
            .getResultList();
        print("rooms = " + rooms);
      });

      step("6. Scalar result");
      emf.runInTransaction(em -> {
        Long free = em.createNamedQuery("Room.countAvailable", Long.class).getSingleResult();
        print("free = " + free);
      });

      step("7. Two columns as Object[] and as a record");
      emf.runInTransaction(em -> {
        List<Object[]> rows = em.createNamedQuery("Room.findRates", Object[].class)
            .setParameter("type", "single")
            .getResultList();
        rows.forEach(r -> print("row = " + Arrays.toString(r)));
        List<RoomRate> rates = em.createNamedQuery("Room.findRates", RoomRate.class)
            .setParameter("type", "single")
            .getResultList();
        print("rates = " + rates);
      });

      step("8. Hibernate @NamedQuery with readOnly");
      emf.runInTransaction(em -> {
        List<Booking> longStays = em.createNamedQuery("Booking.findLongStays", Booking.class)
            .setParameter("nights", 3)
            .getResultList();
        print("longStays = " + longStays.stream().map(Booking::getGuest).toList());
        print("readOnly = " + em.unwrap(Session.class).isReadOnly(longStays.get(0)));
      });

      step("9. @NamedNativeQuery mapped to the entity");
      emf.runInTransaction(em -> {
        List<Room> rooms = em.createNamedQuery("Room.findFreeUnderRate", Room.class)
            .setParameter(1, new BigDecimal("100"))
            .getResultList();
        print("rooms = " + rooms);
      });

      step("10. @NamedNativeQuery mapped to a record");
      emf.runInTransaction(em -> {
        List<RoomCount> counts = em.createNamedQuery("Room.countByType", RoomCount.class)
            .getResultList();
        print("counts = " + counts);
      });

      step("11. Named queries from orm.xml");
      emf.runInTransaction(em -> {
        Long count = em.createNamedQuery("Booking.countByRoom", Long.class)
            .setParameter("number", 301)
            .getSingleResult();
        print("bookings of 301 = " + count);
        List<?> guests = em.createNamedQuery("Booking.guestsSql").getResultList();
        print("guests = " + guests);
      });

      step("12. TypedQueryReference from the static metamodel");
      emf.runInTransaction(em -> {
        List<Room> singles = em.createQuery(Room_._Room_findAvailableByType_)
            .setParameter("type", "single")
            .getResultList();
        print("singles = " + singles);
        Long free = em.createQuery(Room_._Room_countAvailable_).getSingleResult();
        print("free = " + free);
      });

      step("13. TypedQueryReference from the factory");
      print("room queries = " + new TreeMap<>(emf.getNamedQueries(Room.class)).keySet());

      step("14. Generated @HQL query methods");
      emf.runInTransaction(em -> {
        print("available doubles = " + HotelQueries_.findAvailable(em, "double"));
        print("bookings of 102 = " + HotelQueries_.countBookings(em, 102));
      });

      step("15. Update with a named query");
      emf.runInTransaction(em -> {
        int updated = em.createNamedQuery("Room.updateRate")
            .setParameter("rate", new BigDecimal("90.00"))
            .setParameter("type", "single")
            .executeUpdate();
        print("updated = " + updated);
      });

      step("16. Hibernate Session API");
      emf.runInTransaction(em -> {
        Session session = em.unwrap(Session.class);
        List<Room> rooms = session.createNamedSelectionQuery("Room.findAvailableByType", Room.class)
            .setParameter("type", "single")
            .getResultList();
        print("rooms = " + rooms);
        int updated = session.createNamedMutationQuery("Room.updateRate")
            .setParameter("rate", new BigDecimal("130.00"))
            .setParameter("type", "double")
            .executeUpdate();
        print("updated = " + updated);
      });

      step("17. Unknown query name");
      error(() -> emf.runInTransaction(em -> em.createNamedQuery("Room.findCheap", Room.class)));

      step("18. Wrong result class");
      error(() -> emf.runInTransaction(em -> em.createNamedQuery("Room.countAvailable", Room.class)));

      step("19. Native query without resultClass, called with a class");
      error(() -> emf.runInTransaction(em ->
          em.createNamedQuery("Booking.guestsSql", String.class).getResultList()));
    }

    step("20. A typo in a named query stops the startup");
    error(() -> Database.create(false, BrokenQueries.class).close());

    step("21. Startup check switched off");
    error(() -> {
      try (EntityManagerFactory lenient = Database.configuration(false)
          .managedClasses(Room.class, Booking.class, BrokenQueries.class)
          .property("hibernate.query.startup_check", false)
          .createEntityManagerFactory()) {
        print("started");
        lenient.runInTransaction(em -> em.createNamedQuery("Room.findFree", Room.class));
      }
    });

    step("22. A typo in a native named query is found only when it runs");
    error(() -> {
      try (EntityManagerFactory emf = Database.create(true, UncheckedNativeQueries.class)) {
        print("started");
        emf.runInTransaction(em -> em.createNamedQuery("Room.findFreeSql", Room.class).getResultList());
      }
    });
  }

  private static void error(Runnable action) {
    try {
      action.run();
    } catch (RuntimeException e) {
      print(e.getClass().getName() + ": " + e.getMessage());
      for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
        print("Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
      }
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }

  private static void print(String line) {
    System.out.println(line);
  }
}
