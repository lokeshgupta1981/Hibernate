package com.howtodoinjava.hibernate.nativedelete;

import static com.howtodoinjava.hibernate.nativedelete.Database.TODAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.RollbackException;
import jakarta.persistence.TransactionRequiredException;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NativeDeleteTest {

  private EntityManagerFactory emf;

  @BeforeEach
  void setUp() {
    Coupon.REMOVE_CALLBACKS.clear();
    emf = Database.create(false);
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private List<String> codes() {
    return NativeDeleteDemo.codes(emf);
  }

  private long redemptions() {
    return Database.rows(emf, "redemption");
  }

  private int deleteExpired(EntityManager em) {
    return em.createNamedQuery("Coupon.deleteExpired")
        .setParameter("today", TODAY)
        .executeUpdate();
  }

  @Test
  void createNativeQueryDeletesExpiredCoupons() {
    Database.seed(emf, false);
    int deleted = emf.callInTransaction(em -> em
        .createNativeQuery("delete from coupon where expires_on < :today")
        .setParameter("today", TODAY)
        .executeUpdate());
    assertEquals(2, deleted);
    assertEquals(List.of("welcome"), codes());
  }

  @Test
  void namedNativeQueryDeletesExpiredCoupons() {
    Database.seed(emf, false);
    assertEquals(2, emf.callInTransaction(this::deleteExpired));
    assertEquals(List.of("welcome"), codes());
  }

  @Test
  void positionalParameterWorksInNamedNativeQuery() {
    Database.seed(emf, false);
    int deleted = emf.callInTransaction(em -> em
        .createNamedQuery("Coupon.deleteByDiscount")
        .setParameter(1, 50)
        .executeUpdate());
    assertEquals(1, deleted);
    assertEquals(List.of("welcome", "summer"), codes());
  }

  @Test
  void deletingParentWithChildrenFailsOnForeignKey() {
    Database.seed(emf, true);
    ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
        () -> emf.runInTransaction(this::deleteExpired));
    assertTrue(e.getMessage().contains(
        "Referential integrity constraint violation: \"FK_REDEMPTION_COUPON: PUBLIC.REDEMPTION "
            + "FOREIGN KEY(COUPON_ID) REFERENCES PUBLIC.COUPON(ID)"), e.getMessage());
    assertEquals("fk_redemption_coupon", e.getConstraintName().toLowerCase());
    assertEquals(List.of("welcome", "summer", "flash"), codes());
    assertEquals(3, redemptions());
  }

  @Test
  void deletingChildrenFirstFixesTheForeignKeyError() {
    Database.seed(emf, true);
    int[] counts = emf.callInTransaction(em -> {
      int children = em.createNativeQuery("""
              delete from redemption
              where coupon_id in (select id from coupon where expires_on < :today)""")
          .setParameter("today", TODAY)
          .executeUpdate();
      return new int[] {children, deleteExpired(em)};
    });
    assertEquals(2, counts[0]);
    assertEquals(2, counts[1]);
    assertEquals(List.of("welcome"), codes());
    assertEquals(1, redemptions());
  }

  @Test
  void onDeleteCascadeLetsTheDatabaseDeleteChildren() {
    Database.seed(emf, true);
    Database.addOnDeleteCascade(emf);
    assertEquals(2, emf.callInTransaction(this::deleteExpired));
    assertEquals(List.of("welcome"), codes());
    assertEquals(1, redemptions());
  }

  @Test
  void removeCascadesToChildrenAndCallsPreRemove() {
    Database.seed(emf, true);
    emf.runInTransaction(em -> em.remove(NativeDeleteDemo.find(em, "summer")));
    assertEquals(List.of("welcome", "flash"), codes());
    assertEquals(1, redemptions());
    assertEquals(List.of("summer"), Coupon.REMOVE_CALLBACKS);
  }

  @Test
  void nativeDeleteDoesNotCallPreRemove() {
    Database.seed(emf, false);
    emf.runInTransaction(em -> {
      NativeDeleteDemo.find(em, "summer");
      deleteExpired(em);
    });
    assertEquals(List.of("welcome"), codes());
    assertTrue(Coupon.REMOVE_CALLBACKS.isEmpty());
  }

  @Test
  void managedEntityStaysInPersistenceContextAfterNativeDelete() {
    Database.seed(emf, false);
    emf.runInTransaction(em -> {
      Coupon flash = NativeDeleteDemo.find(em, "flash");
      deleteExpired(em);
      assertSame(flash, em.find(Coupon.class, flash.getId()));
      assertTrue(em.contains(flash));
      EntityNotFoundException e = assertThrows(EntityNotFoundException.class, () -> em.refresh(flash));
      assertTrue(e.getMessage().startsWith("No row with the given identifier exists"), e.getMessage());
      em.clear();
      assertNull(em.find(Coupon.class, flash.getId()));
    });
  }

  @Test
  void changingStaleEntityFailsAtCommit() {
    Database.seed(emf, false);
    RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em -> {
      Coupon flash = NativeDeleteDemo.find(em, "flash");
      deleteExpired(em);
      flash.setExpiresOn(LocalDate.of(2026, 11, 30));
    }));
    assertTrue(e.getMessage().contains("Unexpected row count (expected row count 1 but was 0)"),
        e.getMessage());
  }

  @Test
  void pendingChangesAreFlushedBeforeNativeDelete() {
    Database.seed(emf, false);
    int deleted = emf.callInTransaction(em -> {
      NativeDeleteDemo.find(em, "flash").setExpiresOn(LocalDate.of(2026, 11, 30));
      return deleteExpired(em);
    });
    assertEquals(1, deleted);
    assertEquals(List.of("welcome", "flash"), codes());
  }

  @Test
  void jpqlBulkDeleteAlsoFailsOnForeignKeyAndSkipsCallbacks() {
    Database.seed(emf, true);
    assertThrows(ConstraintViolationException.class, () -> emf.runInTransaction(em -> em
        .createQuery("delete from Coupon c where c.expiresOn < :today")
        .setParameter("today", TODAY)
        .executeUpdate()));
    assertEquals(3, codes().size());
  }

  @Test
  void jpqlBulkDeleteSkipsCallbacks() {
    Database.seed(emf, false);
    int deleted = emf.callInTransaction(em -> em
        .createQuery("delete from Coupon c where c.expiresOn < :today")
        .setParameter("today", TODAY)
        .executeUpdate());
    assertEquals(2, deleted);
    assertTrue(Coupon.REMOVE_CALLBACKS.isEmpty());
  }

  @Test
  void nativeDeleteBypassesSoftDelete() {
    emf.runInTransaction(em -> {
      em.persist(new Campaign("Black Friday"));
      em.persist(new Campaign("Back to School"));
      em.persist(new Campaign("New Year"));
    });
    emf.runInTransaction(em -> em.remove(em
        .createQuery("from Campaign where name = 'Black Friday'", Campaign.class).getSingleResult()));
    emf.runInTransaction(em -> em
        .createQuery("delete from Campaign where name = 'Back to School'").executeUpdate());
    assertEquals(3, Database.rows(emf, "campaign"));
    emf.runInTransaction(em -> em
        .createNativeQuery("delete from campaign where name = 'New Year'").executeUpdate());
    assertEquals(2, Database.rows(emf, "campaign"));
    assertEquals(0, Database.count(emf, "Campaign"));
  }

  @Test
  void executeUpdateNeedsATransaction() {
    Database.seed(emf, false);
    try (EntityManager em = emf.createEntityManager()) {
      TransactionRequiredException e = assertThrows(TransactionRequiredException.class,
          () -> deleteExpired(em));
      assertEquals("No active transaction for update or delete query", e.getMessage());
    }
    assertEquals(3, codes().size());
  }
}
