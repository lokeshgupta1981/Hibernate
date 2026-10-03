package com.howtodoinjava.hibernate.onetoone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.PropertyValueException;
import org.hibernate.id.IdentifierGenerationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OneToOneMappingTest {

  private static String nullable(EntityManagerFactory emf, String table, String column) {
    return emf.callInTransaction(em -> (String) em.createNativeQuery(
            "select is_nullable from information_schema.columns where table_name = ?1 and column_name = ?2")
        .setParameter(1, table).setParameter(2, column).getSingleResult());
  }

  private static List<?> columns(EntityManagerFactory emf, String table) {
    return emf.callInTransaction(em -> em.createNativeQuery(
            "select column_name from information_schema.columns where table_name = ?1 order by column_name")
        .setParameter(1, table).getResultList());
  }

  private static long foreignKeys(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> ((Number) em.createNativeQuery(
            "select count(*) from information_schema.table_constraints"
                + " where constraint_type = 'FOREIGN KEY' and table_schema = 'PUBLIC'")
        .getSingleResult()).longValue());
  }

  /** @OneToOne + @JoinColumn, bidirectional with mappedBy, LAZY on both sides. */
  @Nested
  class ForeignKey {

    private EntityManagerFactory emf() {
      return Database.create("fk-test", false,
          com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class,
          com.howtodoinjava.hibernate.onetoone.foreignkey.BoardingPass.class);
    }

    private Long saveLokesh(EntityManagerFactory emf) {
      return emf.callInTransaction(em -> {
        var booking = new com.howtodoinjava.hibernate.onetoone.foreignkey.Booking("Lokesh");
        booking.setBoardingPass(new com.howtodoinjava.hibernate.onetoone.foreignkey.BoardingPass("12A"));
        em.persist(booking);
        return booking.getId();
      });
    }

    @Test
    void persistingTheBookingCascadesToTheBoardingPass() {
      try (var emf = emf()) {
        Long bookingId = saveLokesh(emf);
        Long fk = emf.callInTransaction(em -> em
            .createQuery("select p.booking.id from BoardingPass p", Long.class).getSingleResult());
        assertEquals(bookingId, fk);
        assertEquals(1, Database.count(emf, "BoardingPass"));
      }
    }

    @Test
    void joinColumnIsUniqueAndNotNull() {
      try (var emf = emf()) {
        assertEquals("NO", nullable(emf, "BOARDING_PASS", "BOOKING_ID"));   // optional = false
        Long bookingId = saveLokesh(emf);
        RollbackException e = assertThrows(RollbackException.class, () ->
            emf.runInTransaction(em -> {
              var second = new com.howtodoinjava.hibernate.onetoone.foreignkey.BoardingPass("14C");
              second.setBooking(em.find(com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class, bookingId));
              em.persist(second);
            }));
        assertTrue(e.getMessage().contains("Unique index or primary key violation"));
      }
    }

    @Test
    void owningSideIsLazy() {
      try (var emf = emf()) {
        saveLokesh(emf);
        Long passId = emf.callInTransaction(em -> em
            .createQuery("select p.id from BoardingPass p", Long.class).getSingleResult());
        Database.resetStatements(emf);
        emf.runInTransaction(em -> {
          var pass = em.find(com.howtodoinjava.hibernate.onetoone.foreignkey.BoardingPass.class, passId);
          assertFalse(Hibernate.isInitialized(pass.getBooking()));
          assertEquals(1, Database.statements(emf));
          assertEquals("Lokesh", pass.getBooking().getPassenger());   // first getter loads it
          assertTrue(Hibernate.isInitialized(pass.getBooking()));
          assertEquals(2, Database.statements(emf));
        });
      }
    }

    @Test
    void inverseSideIgnoresLazy() {
      try (var emf = emf()) {
        Long bookingId = saveLokesh(emf);
        Database.resetStatements(emf);
        boolean loaded = emf.callInTransaction(em -> Hibernate.isInitialized(em.find(
            com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class, bookingId).getBoardingPass()));
        assertTrue(loaded);
        assertEquals(2, Database.statements(emf));
      }
    }

    @Test
    void joinFetchLoadsBothInOneStatement() {
      try (var emf = emf()) {
        Long bookingId = saveLokesh(emf);
        Database.resetStatements(emf);
        String seat = emf.callInTransaction(em -> em.createQuery(
                "select b from Booking b join fetch b.boardingPass where b.id = :id",
                com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class)
            .setParameter("id", bookingId).getSingleResult().getBoardingPass().getSeat());
        assertEquals("12A", seat);
        assertEquals(1, Database.statements(emf));
      }
    }

    @Test
    void settingTheBoardingPassToNullDeletesIt() {
      try (var emf = emf()) {
        Long bookingId = saveLokesh(emf);
        emf.runInTransaction(em -> em.find(
            com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class, bookingId).setBoardingPass(null));
        assertEquals(0, Database.count(emf, "BoardingPass"));
        assertEquals(1, Database.count(emf, "Booking"));
      }
    }

    @Test
    void boardingPassWithoutBookingIsRejected() {
      try (var emf = emf()) {
        PropertyValueException e = assertThrows(PropertyValueException.class, () ->
            emf.runInTransaction(em ->
                em.persist(new com.howtodoinjava.hibernate.onetoone.foreignkey.BoardingPass("14C"))));
        assertTrue(e.getMessage().startsWith("not-null property references a null or transient value"));
      }
    }

    @Test
    void deletingTheBookingDeletesTheBoardingPass() {
      try (var emf = emf()) {
        Long bookingId = saveLokesh(emf);
        emf.runInTransaction(em -> em.remove(
            em.find(com.howtodoinjava.hibernate.onetoone.foreignkey.Booking.class, bookingId)));
        assertEquals(0, Database.count(emf, "Booking"));
        assertEquals(0, Database.count(emf, "BoardingPass"));
      }
    }
  }

  /** Same tables with the JPA defaults: EAGER, optional = true. */
  @Nested
  class EagerDefaults {

    private EntityManagerFactory emf() {
      EntityManagerFactory emf = Database.create("eager-test", false,
          com.howtodoinjava.hibernate.onetoone.eager.Booking.class,
          com.howtodoinjava.hibernate.onetoone.eager.BoardingPass.class);
      emf.runInTransaction(em -> {
        for (String[] row : List.of(new String[] {"Lokesh", "12A"},
            new String[] {"Alex", "14C"}, new String[] {"Maria", "3F"})) {
          var booking = new com.howtodoinjava.hibernate.onetoone.eager.Booking(row[0]);
          booking.setBoardingPass(new com.howtodoinjava.hibernate.onetoone.eager.BoardingPass(row[1]));
          em.persist(booking);
        }
      });
      return emf;
    }

    @Test
    void joinColumnIsNullableByDefault() {
      try (var emf = emf()) {
        assertEquals("YES", nullable(emf, "BOARDING_PASS", "BOOKING_ID"));
      }
    }

    @Test
    void findJoinsTheBookingInOneStatement() {
      try (var emf = emf()) {
        Long passId = emf.callInTransaction(em -> em
            .createQuery("select min(p.id) from BoardingPass p", Long.class).getSingleResult());
        Database.resetStatements(emf);
        boolean loaded = emf.callInTransaction(em -> Hibernate.isInitialized(em.find(
            com.howtodoinjava.hibernate.onetoone.eager.BoardingPass.class, passId).getBooking()));
        assertTrue(loaded);
        assertEquals(1, Database.statements(emf));
      }
    }

    @Test
    void jpqlQueryRunsOneExtraSelectPerRow() {
      try (var emf = emf()) {
        Database.resetStatements(emf);
        int rows = emf.callInTransaction(em -> em.createQuery("select p from BoardingPass p",
            com.howtodoinjava.hibernate.onetoone.eager.BoardingPass.class).getResultList().size());
        assertEquals(3, rows);
        assertEquals(4, Database.statements(emf));   // 1 + 3
      }
    }
  }

  /** @MapsId: boarding_pass.booking_id is primary key and foreign key. */
  @Nested
  class SharedPrimaryKey {

    private EntityManagerFactory emf() {
      return Database.create("mapsid-test", false,
          com.howtodoinjava.hibernate.onetoone.sharedkey.Booking.class,
          com.howtodoinjava.hibernate.onetoone.sharedkey.BoardingPass.class);
    }

    private Long saveLokesh(EntityManagerFactory emf) {
      return emf.callInTransaction(em -> {
        var booking = new com.howtodoinjava.hibernate.onetoone.sharedkey.Booking("Lokesh");
        em.persist(booking);
        var pass = new com.howtodoinjava.hibernate.onetoone.sharedkey.BoardingPass(booking, "12A");
        em.persist(pass);
        assertEquals(booking.getId(), pass.getId());
        return booking.getId();
      });
    }

    @Test
    void boardingPassTableHasOnlyBookingIdAndSeat() {
      try (var emf = emf()) {
        assertEquals(List.of("BOOKING_ID", "SEAT"), columns(emf, "BOARDING_PASS"));
        assertEquals(1, foreignKeys(emf));
      }
    }

    @Test
    void boardingPassGetsTheBookingId() {
      try (var emf = emf()) {
        Long bookingId = saveLokesh(emf);
        Long passId = emf.callInTransaction(em -> em
            .createQuery("select p.id from BoardingPass p", Long.class).getSingleResult());
        assertEquals(bookingId, passId);
      }
    }

    @Test
    void findByBookingIdIsOneStatementAndBookingStaysLazy() {
      try (var emf = emf()) {
        Long bookingId = saveLokesh(emf);
        Database.resetStatements(emf);
        emf.runInTransaction(em -> {
          var pass = em.find(com.howtodoinjava.hibernate.onetoone.sharedkey.BoardingPass.class, bookingId);
          assertEquals("12A", pass.getSeat());
          assertFalse(Hibernate.isInitialized(pass.getBooking()));
        });
        assertEquals(1, Database.statements(emf));
      }
    }
  }

  /** @JoinTable: the link is a row in booking_boarding_pass. */
  @Nested
  class JoinTable {

    @Test
    void onlyLinkedBookingsGetARowAndFindUsesOneStatement() {
      try (EntityManagerFactory emf = Database.create("jointable-test", false,
          com.howtodoinjava.hibernate.onetoone.jointable.Booking.class,
          com.howtodoinjava.hibernate.onetoone.jointable.BoardingPass.class)) {
        assertEquals(List.of("BOARDING_PASS_ID", "BOOKING_ID"), columns(emf, "BOOKING_BOARDING_PASS"));
        assertEquals(List.of("ID", "PASSENGER"), columns(emf, "BOOKING"));
        Long bookingId = emf.callInTransaction(em -> {
          var lokesh = new com.howtodoinjava.hibernate.onetoone.jointable.Booking("Lokesh");
          lokesh.setBoardingPass(new com.howtodoinjava.hibernate.onetoone.jointable.BoardingPass("12A"));
          em.persist(lokesh);
          em.persist(new com.howtodoinjava.hibernate.onetoone.jointable.Booking("Alex"));
          return lokesh.getId();
        });
        long links = emf.callInTransaction(em -> ((Number) em
            .createNativeQuery("select count(*) from booking_boarding_pass").getSingleResult()).longValue());
        assertEquals(2, Database.count(emf, "Booking"));
        assertEquals(1, links);

        Database.resetStatements(emf);
        String seat = emf.callInTransaction(em -> em.find(
            com.howtodoinjava.hibernate.onetoone.jointable.Booking.class, bookingId).getBoardingPass().getSeat());
        assertEquals("12A", seat);
        assertEquals(1, Database.statements(emf));
      }
    }
  }

  /** @PrimaryKeyJoinColumn: same key values, but nothing copies the id. */
  @Nested
  class PrimaryKeyJoinColumn {

    private EntityManagerFactory emf() {
      return Database.create("pkjc-test", false,
          com.howtodoinjava.hibernate.onetoone.primarykey.Booking.class,
          com.howtodoinjava.hibernate.onetoone.primarykey.BoardingPass.class);
    }

    @Test
    void idIsNotCopiedAndNoForeignKeyIsCreated() {
      try (var emf = emf()) {
        assertEquals(0, foreignKeys(emf));
        IdentifierGenerationException e = assertThrows(IdentifierGenerationException.class, () ->
            emf.runInTransaction(em -> {
              var booking = new com.howtodoinjava.hibernate.onetoone.primarykey.Booking("Lokesh");
              booking.setBoardingPass(new com.howtodoinjava.hibernate.onetoone.primarykey.BoardingPass("12A"));
              em.persist(booking);
            }));
        assertTrue(e.getMessage().contains("must be manually assigned before calling 'persist()'"));
      }
    }

    @Test
    void worksWhenTheIdIsCopiedByHand() {
      try (var emf = emf()) {
        Long bookingId = emf.callInTransaction(em -> {
          var booking = new com.howtodoinjava.hibernate.onetoone.primarykey.Booking("Lokesh");
          em.persist(booking);
          var pass = new com.howtodoinjava.hibernate.onetoone.primarykey.BoardingPass("12A");
          pass.setId(booking.getId());
          booking.setBoardingPass(pass);
          return booking.getId();
        });
        String seat = emf.callInTransaction(em -> em.find(
            com.howtodoinjava.hibernate.onetoone.primarykey.Booking.class, bookingId).getBoardingPass().getSeat());
        assertEquals("12A", seat);
      }
    }
  }
}
