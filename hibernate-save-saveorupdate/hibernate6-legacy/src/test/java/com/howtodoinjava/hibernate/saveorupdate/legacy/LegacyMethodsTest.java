package com.howtodoinjava.hibernate.saveorupdate.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.OptimisticLockException;
import java.util.List;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.TransientObjectException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Proves the behavior of the old Session methods on Hibernate 6.6, the last version that has them. */
@SuppressWarnings("deprecation")
class LegacyMethodsTest {

  private static final String INSERT = "insert into Plant (name,potSize,price,species,id) values (?,?,?,?,default)";
  private static final String UPDATE = "update Plant set name=?,potSize=?,price=?,species=? where id=?";
  private static final String SELECT =
      "select p1_0.id,p1_0.name,p1_0.potSize,p1_0.price,p1_0.species from Plant p1_0 where p1_0.id=?";

  private SessionFactory sf;
  private Long id;

  @BeforeEach
  void setUp() {
    sf = Database.create();
    Plant monstera = new Plant("Monstera", "Monstera deliciosa", "medium", 25.00);
    sf.inTransaction(s -> s.persist(monstera));
    id = monstera.getId();
    SqlLog.clear();
  }

  @AfterEach
  void tearDown() {
    sf.close();
  }

  private Plant detached() {
    Plant plant = sf.fromTransaction(s -> s.get(Plant.class, id));
    SqlLog.clear();
    return plant;
  }

  private static Plant cactus99() {
    Plant cactus = new Plant("Cactus", "Echinopsis", "small", 6.00);
    cactus.setId(99L);
    return cactus;
  }

  @Test
  void theThreeMethodsAreDeprecatedSince60() throws Exception {
    for (String name : List.of("save", "update", "saveOrUpdate")) {
      Deprecated d = Session.class.getMethod(name, Object.class).getAnnotation(Deprecated.class);
      assertEquals("6.0", d.since(), name);
    }
  }

  @Test
  void saveReturnsTheGeneratedIdAndInsertsAtTheCall() {
    try (Session s = sf.openSession()) {
      Transaction tx = s.beginTransaction();
      Plant basil = new Plant("Basil", "Ocimum basilicum", "small", 4.00);
      Object returned = s.save(basil);
      assertEquals(Long.class, returned.getClass());
      assertEquals(basil.getId(), returned);
      assertTrue(s.contains(basil));
      assertEquals(List.of(INSERT), SqlLog.statements());
      SqlLog.clear();
      tx.commit();
      assertEquals(List.of(), SqlLog.statements());
    }
  }

  @Test
  void saveOnAManagedPlantRunsNoSqlAndReturnsItsId() {
    try (Session s = sf.openSession()) {
      Transaction tx = s.beginTransaction();
      Plant managed = s.get(Plant.class, id);
      SqlLog.clear();
      managed.setPrice(27.00);
      assertEquals(id, s.save(managed));
      assertEquals(List.of(), SqlLog.statements());
      tx.commit();
      assertEquals(List.of(UPDATE), SqlLog.statements());
    }
  }

  @Test
  void saveOnADetachedPlantInsertsADuplicateRow() {
    Plant plant = detached();
    Object newId = sf.fromTransaction(s -> s.save(plant));
    assertNotEquals(id, newId);
    assertEquals(newId, plant.getId());
    assertEquals(List.of(INSERT), SqlLog.statements());
    assertEquals(2, Database.count(sf));
  }

  @Test
  void saveIgnoresAnIdThatDoesNotExist() {
    Plant cactus = cactus99();
    Object newId = sf.fromTransaction(s -> s.save(cactus));
    assertEquals(2L, newId);
    assertEquals(2L, cactus.getId());
    assertEquals(List.of(INSERT), SqlLog.statements());
  }

  @Test
  void updateAttachesTheSameObjectAndAlwaysUpdatesAtFlush() {
    Plant plant = detached();
    try (Session s = sf.openSession()) {
      Transaction tx = s.beginTransaction();
      s.update(plant);
      assertTrue(s.contains(plant));
      assertEquals(List.of(), SqlLog.statements());
      tx.commit();
      assertEquals(List.of(UPDATE), SqlLog.statements());   // nothing changed, UPDATE anyway
    }
  }

  @Test
  void updateOnANewPlantThrowsTransientObjectException() {
    Plant fern = new Plant("Fern", "Nephrolepis exaltata", "medium", 9.00);
    TransientObjectException e = assertThrows(TransientObjectException.class,
        () -> sf.inTransaction(s -> s.update(fern)));
    assertEquals("The given object has a null identifier: " + Plant.class.getName(), e.getMessage());
  }

  @Test
  void updateWhenTheIdIsAlreadyLoadedThrowsNonUniqueObjectException() {
    Plant plant = detached();
    NonUniqueObjectException e = assertThrows(NonUniqueObjectException.class, () -> sf.inTransaction(s -> {
      s.get(Plant.class, id);
      s.update(plant);
    }));
    assertEquals("A different object with the same identifier value was already associated with the session: ["
        + Plant.class.getName() + "#" + id + "]", e.getMessage());
  }

  @Test
  void updateOnAMissingRowFailsAtCommit() {
    try (Session s = sf.openSession()) {
      Transaction tx = s.beginTransaction();
      s.update(cactus99());
      assertEquals(List.of(), SqlLog.statements());
      OptimisticLockException e = assertThrows(OptimisticLockException.class, tx::commit);
      assertInstanceOf(StaleObjectStateException.class, e.getCause());
      assertEquals("Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect): ["
          + Plant.class.getName() + "#99]", e.getMessage());
      assertEquals(List.of(UPDATE), SqlLog.statements());
    }
  }

  @Test
  void saveOrUpdateInsertsANewPlantAtTheCall() {
    try (Session s = sf.openSession()) {
      Transaction tx = s.beginTransaction();
      Plant fern = new Plant("Fern", "Nephrolepis exaltata", "medium", 9.00);
      s.saveOrUpdate(fern);
      assertEquals(2L, fern.getId());
      assertEquals(List.of(INSERT), SqlLog.statements());
      tx.commit();
    }
  }

  @Test
  void saveOrUpdateUpdatesADetachedPlantAtCommit() {
    Plant plant = detached();
    plant.setPrice(30.00);
    try (Session s = sf.openSession()) {
      Transaction tx = s.beginTransaction();
      s.saveOrUpdate(plant);
      assertTrue(s.contains(plant));
      assertEquals(List.of(), SqlLog.statements());
      tx.commit();
      assertEquals(List.of(UPDATE), SqlLog.statements());
    }
    assertEquals(30.00, sf.fromTransaction(s -> s.get(Plant.class, id)).getPrice());
  }

  @Test
  void saveOrUpdateOnAMissingRowFailsAtCommit() {
    try (Session s = sf.openSession()) {
      Transaction tx = s.beginTransaction();
      s.saveOrUpdate(cactus99());
      assertEquals(List.of(), SqlLog.statements());
      assertThrows(OptimisticLockException.class, tx::commit);
    }
  }

  @Test
  void saveOrUpdateWhenTheIdIsAlreadyLoadedThrowsNonUniqueObjectException() {
    Plant plant = detached();
    assertThrows(NonUniqueObjectException.class, () -> sf.inTransaction(s -> {
      s.get(Plant.class, id);
      s.saveOrUpdate(plant);
    }));
  }

  @Test
  void persistOnADetachedPlantThrowsEntityExistsException() {
    Plant plant = detached();
    EntityExistsException e = assertThrows(EntityExistsException.class,
        () -> sf.inTransaction(s -> s.persist(plant)));
    assertEquals("detached entity passed to persist: " + Plant.class.getName(), e.getMessage());
  }

  @Test
  void mergeOnAMissingRowFailsAtTheCall() {
    try (Session s = sf.openSession()) {
      s.beginTransaction();
      assertThrows(OptimisticLockException.class, () -> s.merge(cactus99()));
      assertTrue(SqlLog.statements().contains(SELECT));
    }
  }
}
