package com.howtodoinjava.hibernate.insert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import java.util.Arrays;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InsertTest {

  private EntityManagerFactory emf;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    SqlLog.clear();
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private static long inserts(String table) {
    return SqlLog.statements().stream().filter(s -> s.startsWith("insert into " + table)).count();
  }

  @Test
  void sequenceIdIsAssignedAtPersistAndInsertRunsAtCommit() {
    Pet max = new Pet("Max", "dog", 3);
    emf.runInTransaction(em -> {
      assertNull(max.getId());
      assertFalse(em.contains(max));
      em.persist(max);
      assertEquals(1L, max.getId());
      assertTrue(em.contains(max));
      assertEquals(List.of("select next value for Pet_SEQ"), SqlLog.statements());
      assertEquals(0, inserts("Pet"));
    });
    assertEquals(1, inserts("Pet"));
    assertEquals(1, Database.count(emf, "Pet"));
  }

  @Test
  void identityInsertRunsAtPersist() {
    Adopter lokesh = new Adopter("Lokesh");
    emf.runInTransaction(em -> {
      em.persist(lokesh);
      assertEquals(1, inserts("Adopter"));
      assertEquals("insert into Adopter (name,id) values (?,default)", SqlLog.statements().get(0));
      assertEquals(1L, lokesh.getId());
    });
  }

  @Test
  void assignedIdSendsNoSqlUntilCommit() {
    emf.runInTransaction(em -> {
      em.persist(new Kennel(1, "large"));
      assertTrue(SqlLog.statements().isEmpty());
    });
    assertEquals(List.of("insert into Kennel (size,number) values (?,?)"), SqlLog.statements());
  }

  @Test
  void flushSendsInsertBeforeCommit() {
    emf.runInTransaction(em -> {
      em.persist(new Pet("Luna", "cat", 2));
      assertEquals(0, inserts("Pet"));
      em.flush();
      assertEquals(1, inserts("Pet"));
    });
  }

  @Test
  void queryFlushesPendingInserts() {
    emf.runInTransaction(em -> {
      em.persist(new Pet("Coco", "rabbit", 1));
      long pets = em.createQuery("select count(*) from Pet", Long.class).getSingleResult();
      assertEquals(1, pets);
      assertEquals(1, inserts("Pet"));
    });
  }

  @Test
  void pooledSequenceIsCalledTwiceForTheFirstFiftyIds() {
    emf.runInTransaction(em -> {
      for (int i = 1; i <= 50; i++) {
        em.persist(new Pet("Pet " + i, "dog", 1));
      }
    });
    assertEquals(2, SqlLog.statements().stream().filter(s -> s.contains("Pet_SEQ")).count());
    SqlLog.clear();
    emf.runInTransaction(em -> em.persist(new Pet("Max", "dog", 3)));
    assertEquals(0, SqlLog.statements().stream().filter(s -> s.contains("Pet_SEQ")).count());
    SqlLog.clear();
    emf.runInTransaction(em -> {
      for (int i = 52; i <= 101; i++) {
        em.persist(new Pet("Pet " + i, "cat", 1));
      }
    });
    assertEquals(1, SqlLog.statements().stream().filter(s -> s.contains("Pet_SEQ")).count());
  }

  @Test
  void generatedIdFromEntityOrPersistenceUnitUtil() {
    Pet daisy = new Pet("Daisy", "dog", 5);
    Long id = emf.callInTransaction(em -> {
      em.persist(daisy);
      return daisy.getId();
    });
    assertEquals(1L, id);
    assertEquals(1L, emf.getPersistenceUnitUtil().getIdentifier(daisy));
  }

  @Test
  void persistingDetachedEntityFails() {
    Pet max = new Pet("Max", "dog", 3);
    emf.runInTransaction(em -> em.persist(max));
    EntityExistsException e = assertThrows(EntityExistsException.class,
        () -> emf.runInTransaction(em -> em.persist(max)));
    assertEquals("Detached entity passed to persist: com.howtodoinjava.hibernate.insert.Pet", e.getMessage());
    assertEquals(1, Database.count(emf, "Pet"));
  }

  @Test
  void persistingNewEntityWithIdSetByHandFails() {
    Pet ghost = new Pet("Ghost", "dog", 1);
    ghost.setId(77L);
    EntityExistsException e = assertThrows(EntityExistsException.class,
        () -> emf.runInTransaction(em -> em.persist(ghost)));
    assertEquals("Detached entity passed to persist: com.howtodoinjava.hibernate.insert.Pet", e.getMessage());
    assertEquals(0, Database.count(emf, "Pet"));
  }

  @Test
  void mergeDetachedEntityUpdatesRowAndReturnsCopy() {
    Pet max = new Pet("Max", "dog", 3);
    emf.runInTransaction(em -> em.persist(max));
    max.setAge(4);
    SqlLog.clear();
    emf.runInTransaction(em -> {
      Pet copy = em.merge(max);
      assertNotSame(max, copy);
      assertTrue(em.contains(copy));
      assertFalse(em.contains(max));
    });
    List<String> sql = SqlLog.statements();
    assertTrue(sql.get(0).startsWith("select"));
    assertEquals("update Pet set adopted=?,age=?,name=?,species=? where id=?", sql.get(1));
    int age = emf.callInTransaction(em -> em.find(Pet.class, max.getId()).getAge());
    assertEquals(4, age);
  }

  @Test
  void mergeNewEntityInsertsCopyAndLeavesOriginalWithoutId() {
    Pet bella = new Pet("Bella", "cat", 1);
    Pet copy = emf.callInTransaction(em -> em.merge(bella));
    assertNull(bella.getId());
    assertEquals(1L, copy.getId());
    assertEquals(1, inserts("Pet"));
  }

  @Test
  void duplicateIdInSamePersistenceContext() {
    EntityExistsException e = assertThrows(EntityExistsException.class, () -> emf.runInTransaction(em -> {
      em.persist(new Kennel(2, "small"));
      em.persist(new Kennel(2, "medium"));
    }));
    assertEquals("A different object with the same identifier value was already associated with this "
        + "persistence context for entity [com.howtodoinjava.hibernate.insert.Kennel with id '2']", e.getMessage());
    assertEquals(0, Database.count(emf, "Kennel"));
  }

  @Test
  void duplicateIdInDatabaseFailsAtCommit() {
    emf.runInTransaction(em -> em.persist(new Kennel(1, "large")));
    RollbackException e = assertThrows(RollbackException.class,
        () -> emf.runInTransaction(em -> em.persist(new Kennel(1, "small"))));
    assertInstanceOf(ConstraintViolationException.class, e.getCause());
    assertTrue(e.getMessage().contains("Unique index or primary key violation"));
  }

  @Test
  void hqlInsertValues() {
    int rows = emf.callInTransaction(em -> em.createQuery("""
            insert Pet (name, species, age, adopted)
            values ('Rocky', 'dog', 4, false), ('Milo', 'cat', 3, false)""")
        .executeUpdate());
    assertEquals(2, rows);
    assertTrue(SqlLog.statements().contains(
        "insert into Pet(name,species,age,adopted,id) values ('Rocky','dog',4,false,?), ('Milo','cat',3,false,?)"));
    assertEquals(List.of(1L, 2L), emf.callInTransaction(em ->
        em.createQuery("select id from Pet order by id", Long.class).getResultList()));
  }

  @Test
  void hqlInsertSelect() {
    emf.runInTransaction(em -> {
      Pet max = new Pet("Max", "dog", 3);
      max.setAdopted(true);
      Pet daisy = new Pet("Daisy", "dog", 5);
      daisy.setAdopted(true);
      em.persist(max);
      em.persist(daisy);
      em.persist(new Pet("Luna", "cat", 2));
    });
    int rows = emf.callInTransaction(em -> em.createQuery("""
            insert into AdoptedPet (name, species)
            select p.name, p.species from Pet p where p.adopted = true""")
        .executeUpdate());
    assertEquals(2, rows);
    assertTrue(SqlLog.statements().contains(
        "insert into AdoptedPet(name,species) select p1_0.name,p1_0.species from Pet p1_0 where p1_0.adopted=true"));
    assertEquals(List.of("Daisy", "Max"), emf.callInTransaction(em ->
        em.createQuery("select name from AdoptedPet order by name", String.class).getResultList()));
  }

  @Test
  void nativeInsert() {
    int rows = emf.callInTransaction(em -> em
        .createNativeQuery("insert into Kennel (number, size) values (?, ?)")
        .setParameter(1, 3)
        .setParameter(2, "medium")
        .executeUpdate());
    assertEquals(1, rows);
    assertEquals("medium", emf.callInTransaction(em -> em.find(Kennel.class, 3).getSize()));
  }

  @Test
  void sessionPersistAndStatelessInsert() {
    emf.runInTransaction(em -> em.unwrap(Session.class).persist(new Pet("Oscar", "parrot", 6)));
    Object id = emf.unwrap(SessionFactory.class)
        .fromStatelessTransaction(ss -> ss.insert(new Pet("Oliver", "cat", 2)));
    assertEquals(2L, id);
    assertEquals(2, Database.count(emf, "Pet"));
  }

  @Test
  void sessionHasNoSaveMethodsInHibernate7() {
    List<String> names = Arrays.stream(Session.class.getMethods()).map(m -> m.getName()).toList();
    assertFalse(names.contains("save"));
    assertFalse(names.contains("saveOrUpdate"));
    assertFalse(names.contains("update"));
    assertTrue(names.contains("persist"));
    assertTrue(names.contains("merge"));
  }

  @Test
  void persistWithoutTransactionWritesNothing() {
    try (EntityManager em = emf.createEntityManager()) {
      em.persist(new Pet("Ghost", "dog", 1));
      em.persist(new Adopter("Ghost"));
    }
    assertEquals(0, inserts("Pet"));
    assertEquals(0, inserts("Adopter"));
    assertEquals(0, Database.count(emf, "Pet"));
    assertEquals(0, Database.count(emf, "Adopter"));
  }

  @Test
  void rollbackDiscardsIdentityInsert() {
    assertThrows(IllegalStateException.class, () -> emf.runInTransaction(em -> {
      em.persist(new Adopter("Anna"));
      throw new IllegalStateException("adoption form incomplete");
    }));
    assertEquals(1, inserts("Adopter"));
    assertEquals(0, Database.count(emf, "Adopter"));
  }
}
