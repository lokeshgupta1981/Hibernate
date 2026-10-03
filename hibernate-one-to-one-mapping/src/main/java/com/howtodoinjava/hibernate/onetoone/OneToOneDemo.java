package com.howtodoinjava.hibernate.onetoone;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.Hibernate;

public class OneToOneDemo {

  public static void main(String[] args) {
    foreignKey();
    eagerDefaults();
    sharedPrimaryKey();
    joinTable();
    primaryKeyJoinColumn();
  }

  // 1. @OneToOne + @JoinColumn, bidirectional with mappedBy, LAZY on both sides
  private static void foreignKey() {
    title("FOREIGN KEY: boarding_pass.booking_id -> booking.id");
    try (EntityManagerFactory emf = Database.create("foreignkey", true,
        com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class,
        com.howtodoinjava.hibernate.onetoone.foreignkey.BoardingPass.class)) {

      step("1. Save a booking with its boarding pass (cascade from Booking)");
      Long bookingId = emf.callInTransaction(em -> {
        var booking = new com.howtodoinjava.hibernate.onetoone.foreignkey.Booking("Lokesh");
        booking.setBoardingPass(new com.howtodoinjava.hibernate.onetoone.foreignkey.BoardingPass("12A"));
        em.persist(booking);
        return booking.getId();
      });
      Long passId = emf.callInTransaction(em -> em
          .createQuery("select p.id from BoardingPass p", Long.class).getSingleResult());

      step("2. Find the boarding pass (owning side, LAZY)");
      Database.resetStatements(emf);
      emf.runInTransaction(em -> {
        var pass = em.find(com.howtodoinjava.hibernate.onetoone.foreignkey.BoardingPass.class, passId);
        System.out.println("booking loaded? " + Hibernate.isInitialized(pass.getBooking()));
      });
      System.out.println("statements=" + Database.statements(emf));

      step("3. Find the booking (inverse side, LAZY is ignored)");
      Database.resetStatements(emf);
      emf.runInTransaction(em -> {
        var booking = em.find(com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class, bookingId);
        System.out.println("pass loaded? " + Hibernate.isInitialized(booking.getBoardingPass()));
      });
      System.out.println("statements=" + Database.statements(emf));

      step("4. Read the seat through the booking with join fetch");
      Database.resetStatements(emf);
      String seat = emf.callInTransaction(em -> em.createQuery(
              "select b from Booking b join fetch b.boardingPass where b.id = :id",
              com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class)
          .setParameter("id", bookingId).getSingleResult().getBoardingPass().getSeat());
      System.out.println("seat=" + seat + " statements=" + Database.statements(emf));

      step("5. Save a second boarding pass for the same booking (unique booking_id)");
      try {
        emf.runInTransaction(em -> {
          var second = new com.howtodoinjava.hibernate.onetoone.foreignkey.BoardingPass("14C");
          second.setBooking(em.find(com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class, bookingId));
          em.persist(second);
        });
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("6. Set the boarding pass to null (orphanRemoval = true)");
      emf.runInTransaction(em -> em.find(
          com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class, bookingId).setBoardingPass(null));
      System.out.println("boarding passes=" + Database.count(emf, "BoardingPass"));

      step("7. Save a boarding pass without a booking (optional = false)");
      try {
        emf.runInTransaction(em ->
            em.persist(new com.howtodoinjava.hibernate.onetoone.foreignkey.BoardingPass("14C")));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("8. Delete a booking that has a boarding pass (cascade = ALL)");
      Long alexId = emf.callInTransaction(em -> {
        var alex = new com.howtodoinjava.hibernate.onetoone.foreignkey.Booking("Alex");
        alex.setBoardingPass(new com.howtodoinjava.hibernate.onetoone.foreignkey.BoardingPass("14C"));
        em.persist(alex);
        return alex.getId();
      });
      emf.runInTransaction(em ->
          em.remove(em.find(com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class, alexId)));
      System.out.println("bookings=" + Database.count(emf, "Booking")
          + " boarding passes=" + Database.count(emf, "BoardingPass"));
    }
  }

  // 2. Same tables, JPA defaults: EAGER on both sides
  private static void eagerDefaults() {
    title("DEFAULT FETCH TYPE: EAGER");
    try (EntityManagerFactory emf = Database.create("eager", true,
        com.howtodoinjava.hibernate.onetoone.eager.Booking.class,
        com.howtodoinjava.hibernate.onetoone.eager.BoardingPass.class)) {

      emf.runInTransaction(em -> {
        for (String[] row : List.of(new String[] {"Lokesh", "12A"},
            new String[] {"Alex", "14C"}, new String[] {"Maria", "3F"})) {
          var booking = new com.howtodoinjava.hibernate.onetoone.eager.Booking(row[0]);
          booking.setBoardingPass(new com.howtodoinjava.hibernate.onetoone.eager.BoardingPass(row[1]));
          em.persist(booking);
        }
      });
      Long passId = emf.callInTransaction(em -> em
          .createQuery("select min(p.id) from BoardingPass p", Long.class).getSingleResult());

      step("9. Find one boarding pass (EAGER booking)");
      Database.resetStatements(emf);
      emf.runInTransaction(em ->
          em.find(com.howtodoinjava.hibernate.onetoone.eager.BoardingPass.class, passId));
      System.out.println("statements=" + Database.statements(emf));

      step("10. Query all boarding passes with JPQL (EAGER booking)");
      Database.resetStatements(emf);
      emf.runInTransaction(em -> em.createQuery("select p from BoardingPass p",
          com.howtodoinjava.hibernate.onetoone.eager.BoardingPass.class).getResultList());
      System.out.println("statements=" + Database.statements(emf));
    }
  }

  // 3. @MapsId: boarding_pass.booking_id is the primary key and the foreign key
  private static void sharedPrimaryKey() {
    title("SHARED PRIMARY KEY: @MapsId");
    try (EntityManagerFactory emf = Database.create("sharedkey", true,
        com.howtodoinjava.hibernate.onetoone.sharedkey.Booking.class,
        com.howtodoinjava.hibernate.onetoone.sharedkey.BoardingPass.class)) {

      step("11. Save a booking, then its boarding pass");
      Long bookingId = emf.callInTransaction(em -> {
        var booking = new com.howtodoinjava.hibernate.onetoone.sharedkey.Booking("Lokesh");
        em.persist(booking);
        var pass = new com.howtodoinjava.hibernate.onetoone.sharedkey.BoardingPass(booking, "12A");
        em.persist(pass);
        System.out.println("booking.id=" + booking.getId() + " pass.id=" + pass.getId());
        return booking.getId();
      });

      step("12. Find the boarding pass by the booking id");
      Database.resetStatements(emf);
      emf.runInTransaction(em -> {
        var pass = em.find(com.howtodoinjava.hibernate.onetoone.sharedkey.BoardingPass.class, bookingId);
        System.out.println("seat=" + pass.getSeat()
            + " booking loaded? " + Hibernate.isInitialized(pass.getBooking()));
      });
      System.out.println("statements=" + Database.statements(emf));
    }
  }

  // 4. @JoinTable: the link is a row in booking_boarding_pass
  private static void joinTable() {
    title("JOIN TABLE: booking_boarding_pass");
    try (EntityManagerFactory emf = Database.create("jointable", true,
        com.howtodoinjava.hibernate.onetoone.jointable.Booking.class,
        com.howtodoinjava.hibernate.onetoone.jointable.BoardingPass.class)) {

      step("13. Save a booking with a boarding pass and a booking without one");
      Long bookingId = emf.callInTransaction(em -> {
        var lokesh = new com.howtodoinjava.hibernate.onetoone.jointable.Booking("Lokesh");
        lokesh.setBoardingPass(new com.howtodoinjava.hibernate.onetoone.jointable.BoardingPass("12A"));
        em.persist(lokesh);
        em.persist(new com.howtodoinjava.hibernate.onetoone.jointable.Booking("Alex"));
        return lokesh.getId();
      });
      long links = emf.callInTransaction(em -> ((Number) em
          .createNativeQuery("select count(*) from booking_boarding_pass").getSingleResult()).longValue());
      System.out.println("bookings=" + Database.count(emf, "Booking") + " links=" + links);

      step("14. Find the booking");
      emf.runInTransaction(em -> System.out.println("seat=" + em.find(
          com.howtodoinjava.hibernate.onetoone.jointable.Booking.class, bookingId).getBoardingPass().getSeat()));
    }
  }

  // 5. The older @PrimaryKeyJoinColumn mapping: the id is not copied
  private static void primaryKeyJoinColumn() {
    title("@PrimaryKeyJoinColumn");
    try (EntityManagerFactory emf = Database.create("primarykey", true,
        com.howtodoinjava.hibernate.onetoone.primarykey.Booking.class,
        com.howtodoinjava.hibernate.onetoone.primarykey.BoardingPass.class)) {

      step("15. Save without setting the boarding pass id");
      try {
        emf.runInTransaction(em -> {
          var booking = new com.howtodoinjava.hibernate.onetoone.primarykey.Booking("Lokesh");
          booking.setBoardingPass(new com.howtodoinjava.hibernate.onetoone.primarykey.BoardingPass("12A"));
          em.persist(booking);
        });
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("16. Save with the id copied by hand");
      emf.runInTransaction(em -> {
        var booking = new com.howtodoinjava.hibernate.onetoone.primarykey.Booking("Lokesh");
        em.persist(booking);
        var pass = new com.howtodoinjava.hibernate.onetoone.primarykey.BoardingPass("12A");
        pass.setId(booking.getId());
        booking.setBoardingPass(pass);
      });
      System.out.println("boarding passes=" + Database.count(emf, "BoardingPass"));
    }
  }

  private static void title(String title) {
    System.out.println();
    System.out.println("################ " + title + " ################");
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
