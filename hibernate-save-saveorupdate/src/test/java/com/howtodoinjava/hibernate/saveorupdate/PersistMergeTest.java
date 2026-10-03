package com.howtodoinjava.hibernate.saveorupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import java.util.Arrays;
import java.util.List;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersistMergeTest {

  private static final String INSERT = "insert into Plant (name,potSize,price,species,id) values (?,?,?,?,default)";
  private static final String UPDATE = "update Plant set name=?,potSize=?,price=?,species=? where id=?";
  private static final String SELECT =
      "select p1_0.id,p1_0.name,p1_0.potSize,p1_0.price,p1_0.species from Plant p1_0 where p1_0.id=?";

  private EntityManagerFactory emf;
  private Long id;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    Plant monstera = new Plant("Monstera", "Monstera deliciosa", "medium", 25.00);
    emf.runInTransaction(em -> em.persist(monstera));
    id = monstera.getId();
    SqlLog.clear();
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private Plant detached() {
    Plant plant = emf.callInTransaction(em -> em.find(Plant.class, id));
    SqlLog.clear();
    return plant;
  }

  private static Plant cactus99() {
    Plant cactus = new Plant("Cactus", "Echinopsis", "small", 6.00);
    cactus.setId(99L);
    return cactus;
  }

  /** Runs work in a transaction, asserts the SQL sent during the call and at commit. */
  private void inTransaction(java.util.function.Consumer<EntityManager> work, List<String> atCall,
      List<String> atCommit) {
    try (EntityManager em = emf.createEntityManager()) {
      em.getTransaction().begin();
      work.accept(em);
      assertEquals(atCall, SqlLog.statements(), "SQL at the call");
      SqlLog.clear();
      em.getTransaction().commit();
      assertEquals(atCommit, SqlLog.statements(), "SQL at commit");
    }
  }

  // ---- API status in Hibernate 7.4 ----

  @Test
  void saveUpdateAndSaveOrUpdateAreGoneFromSession() {
    List<String> names = Arrays.stream(Session.class.getMethods()).map(m -> m.getName()).toList();
    assertFalse(names.contains("save"));
    assertFalse(names.contains("update"));
    assertFalse(names.contains("saveOrUpdate"));
    assertTrue(names.contains("persist"));
    assertTrue(names.contains("merge"));
  }

  // ---- New plant ----

  @Test
  void persistInsertsTheSameObject() {
    Plant basil = new Plant("Basil", "Ocimum basilicum", "small", 4.00);
    inTransaction(em -> {
      em.persist(basil);
      assertEquals(2L, basil.getId());
      assertTrue(em.contains(basil));
    }, List.of(INSERT), List.of());
  }

  @Test
  void mergeInsertsACopyAndLeavesTheArgumentWithoutId() {
    Plant fern = new Plant("Fern", "Nephrolepis exaltata", "medium", 9.00);
    inTransaction(em -> {
      Plant copy = em.merge(fern);
      assertNotSame(fern, copy);
      assertEquals(2L, copy.getId());
      assertNull(fern.getId());
      assertFalse(em.contains(fern));
      assertTrue(em.contains(copy));
    }, List.of(INSERT), List.of());
  }

  // ---- Managed plant ----

  @Test
  void persistOnAManagedPlantIsIgnoredAndDirtyCheckingUpdates() {
    try (EntityManager em = emf.createEntityManager()) {
      em.getTransaction().begin();
      Plant managed = em.find(Plant.class, id);
      SqlLog.clear();
      managed.setPrice(27.00);
      em.persist(managed);
      assertEquals(List.of(), SqlLog.statements());
      em.getTransaction().commit();
      assertEquals(List.of(UPDATE), SqlLog.statements());
    }
  }

  @Test
  void mergeOnAManagedPlantReturnsItWithoutSql() {
    try (EntityManager em = emf.createEntityManager()) {
      em.getTransaction().begin();
      Plant managed = em.find(Plant.class, id);
      SqlLog.clear();
      assertSame(managed, em.merge(managed));
      em.getTransaction().commit();
      assertEquals(List.of(), SqlLog.statements());
    }
  }

  // ---- Detached plant ----

  @Test
  void persistOnADetachedPlantThrowsEntityExistsException() {
    Plant plant = detached();
    try (EntityManager em = emf.createEntityManager()) {
      em.getTransaction().begin();
      EntityExistsException e = assertThrows(EntityExistsException.class, () -> em.persist(plant));   // at the call
      assertEquals("Detached entity passed to persist: " + Plant.class.getName(), e.getMessage());
      assertEquals(List.of(), SqlLog.statements());
      em.getTransaction().rollback();
    }
    assertEquals(1, Database.count(emf));
  }

  @Test
  void mergeOnAnUnchangedDetachedPlantSelectsButDoesNotUpdate() {
    Plant plant = detached();
    inTransaction(em -> em.merge(plant), List.of(SELECT), List.of());
  }

  @Test
  void mergeOnAChangedDetachedPlantReturnsAManagedCopy() {
    Plant plant = detached();
    plant.setPotSize("large");
    inTransaction(em -> {
      Plant copy = em.merge(plant);
      assertNotSame(plant, copy);
      assertFalse(em.contains(plant));
      assertTrue(em.contains(copy));
      assertEquals("large", copy.getPotSize());
    }, List.of(SELECT), List.of(UPDATE));
    assertEquals("large", emf.callInTransaction(em -> em.find(Plant.class, id)).getPotSize());
  }

  @Test
  void changesToTheMergeArgumentAfterTheCallAreLost() {
    Plant plant = detached();
    emf.runInTransaction(em -> {
      Plant copy = em.merge(plant);
      plant.setPrice(99.00);   // argument: not saved
      copy.setPotSize("large");   // copy: saved
    });
    Plant row = emf.callInTransaction(em -> em.find(Plant.class, id));
    assertEquals(25.00, row.getPrice());
    assertEquals("large", row.getPotSize());
  }

  @Test
  void mergeCopiesOntoAnAlreadyLoadedInstanceWithoutException() {
    Plant plant = detached();
    plant.setPrice(30.00);
    try (EntityManager em = emf.createEntityManager()) {
      em.getTransaction().begin();
      Plant loaded = em.find(Plant.class, id);
      SqlLog.clear();
      Plant copy = em.merge(plant);
      assertSame(loaded, copy);
      assertEquals(30.00, loaded.getPrice());
      assertEquals(List.of(), SqlLog.statements());
      em.getTransaction().commit();
      assertEquals(List.of(UPDATE), SqlLog.statements());
    }
  }

  // ---- Id that does not exist ----

  @Test
  void persistWithAnIdThatDoesNotExistThrowsEntityExistsException() {
    Plant cactus = cactus99();
    EntityExistsException e = assertThrows(EntityExistsException.class,
        () -> emf.runInTransaction(em -> em.persist(cactus)));
    assertEquals("Detached entity passed to persist: " + Plant.class.getName(), e.getMessage());
    assertEquals(List.of(), SqlLog.statements());
  }

  @Test
  void mergeWithAnIdThatDoesNotExistThrowsOptimisticLockExceptionAtTheCall() {
    try (EntityManager em = emf.createEntityManager()) {
      em.getTransaction().begin();
      OptimisticLockException e = assertThrows(OptimisticLockException.class, () -> em.merge(cactus99()));
      assertEquals("Row was already updated or deleted by another transaction for entity ["
          + Plant.class.getName() + " with id '99']", e.getMessage());
      assertEquals(List.of(SELECT, SELECT), SqlLog.statements());
      em.getTransaction().rollback();
    }
    assertEquals(1, Database.count(emf));
  }

  // ---- saveOrUpdate() replacement ----

  @Test
  void plantStoreSavePersistsANewPlantAndMergesADetachedOne() {
    Plant aloe = new Plant("Aloe", "Aloe vera", "small", 7.50);
    inTransaction(em -> assertSame(aloe, PlantStore.save(em, aloe)), List.of(INSERT), List.of());

    aloe.setPrice(8.00);
    inTransaction(em -> {
      Plant saved = PlantStore.save(em, aloe);
      assertNotSame(aloe, saved);
      assertEquals(8.00, saved.getPrice());
    }, List.of(SELECT), List.of(UPDATE));
  }
}
