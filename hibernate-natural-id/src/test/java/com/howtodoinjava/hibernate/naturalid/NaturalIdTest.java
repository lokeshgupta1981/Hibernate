package com.howtodoinjava.hibernate.naturalid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.HibernateException;
import org.hibernate.KeyType;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NaturalIdTest {

  private final SqlLog sql = new SqlLog();
  private EntityManagerFactory emf;

  @BeforeEach
  void setUp() {
    emf = Database.create(false, false, sql);
    emf.runInTransaction(em -> {
      em.persist(new Vehicle("ABC-123", "Civic"));
      em.persist(new Vehicle("XYZ-789", "Corolla"));
      em.persist(new Vehicle("JKL-456", "Model 3"));
      em.persist(new ParkingSpot("Downtown", 14, true));
      em.persist(new ParkingSpot("Downtown", 15, false));
      em.persist(new ParkingSpot("Airport", 14, false));
    });
    reset();
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private void reset() {
    sql.clear();
    Database.resetStatistics(emf);
  }

  @Test
  void schemaHasUniqueConstraintOnNaturalIdColumns() {
    List<?> rows = emf.callInTransaction(em -> em.createNativeQuery("""
        select k.TABLE_NAME, k.COLUMN_NAME
        from INFORMATION_SCHEMA.TABLE_CONSTRAINTS c
        join INFORMATION_SCHEMA.KEY_COLUMN_USAGE k on k.CONSTRAINT_NAME = c.CONSTRAINT_NAME
        where c.CONSTRAINT_TYPE = 'UNIQUE'
        order by k.TABLE_NAME, k.ORDINAL_POSITION""").getResultList());
    List<String> columns = rows.stream()
        .map(r -> ((Object[]) r)[0] + "." + ((Object[]) r)[1])
        .toList();
    assertEquals(List.of("DRIVER.LICENSENUMBER", "PARKINGSPOT.GARAGE", "PARKINGSPOT.NUMBER",
        "VEHICLE.LICENSEPLATE"), columns);
  }

  @Test
  void findByNaturalIdRunsOneSelectAndThenUsesThePersistenceContext() {
    emf.runInTransaction(em -> {
      Vehicle civic = em.find(Vehicle.class, "ABC-123", KeyType.NATURAL);
      Vehicle again = em.find(Vehicle.class, "ABC-123", KeyType.NATURAL);
      Vehicle byId = em.find(Vehicle.class, civic.getId());
      assertEquals("Civic", civic.getModel());
      assertSame(civic, again);
      assertSame(civic, byId);
    });
    assertEquals(1, Database.statements(emf));
    assertEquals(List.of("select v1_0.id,v1_0.licensePlate,v1_0.model from Vehicle v1_0 where v1_0.licensePlate=?"),
        sql.statements());
  }

  @Test
  void newEntityManagerWithoutSecondLevelCacheRunsTheSelectAgain() {
    emf.runInTransaction(em -> em.find(Vehicle.class, "ABC-123", KeyType.NATURAL));
    emf.runInTransaction(em -> em.find(Vehicle.class, "ABC-123", KeyType.NATURAL));
    assertEquals(2, Database.statements(emf));
  }

  @Test
  void unknownNaturalIdReturnsNull() {
    emf.runInTransaction(em -> {
      assertNull(em.find(Vehicle.class, "NOP-000", KeyType.NATURAL));
      assertNull(em.unwrap(Session.class).bySimpleNaturalId(Vehicle.class).load("NOP-000"));
    });
  }

  @Test
  @SuppressWarnings("deprecation")
  void legacyLoadAccessApisStillWork() {
    emf.runInTransaction(em -> {
      Session session = em.unwrap(Session.class);
      Vehicle corolla = session.bySimpleNaturalId(Vehicle.class).load("XYZ-789");
      assertEquals("Corolla", corolla.getModel());
      assertTrue(session.bySimpleNaturalId(Vehicle.class).loadOptional("NOP-000").isEmpty());
      List<Vehicle> two = session.byMultipleNaturalId(Vehicle.class).multiLoad("ABC-123", "JKL-456");
      assertEquals(List.of("Civic", "Model 3"), two.stream().map(Vehicle::getModel).toList());
    });
    emf.runInTransaction(em -> em.persist(new Driver("L-100", "Lokesh")));
    emf.runInTransaction(em -> {
      Session session = em.unwrap(Session.class);
      Driver lokesh = session.bySimpleNaturalId(Driver.class).load("L-100");
      lokesh.setLicenseNumber("L-200");
      assertNull(session.bySimpleNaturalId(Driver.class).setSynchronizationEnabled(false).load("L-200"));
      assertSame(lokesh, session.bySimpleNaturalId(Driver.class).load("L-200"));
    });
    assertEquals("select v1_0.id,v1_0.licensePlate,v1_0.model from Vehicle v1_0 where v1_0.licensePlate=?",
        sql.statements().getFirst());
  }

  @Test
  @SuppressWarnings("deprecation")
  void legacyGetReferenceSelectsOnlyTheIdAndReturnsAProxy() {
    emf.runInTransaction(em -> {
      Vehicle ref = em.unwrap(Session.class).bySimpleNaturalId(Vehicle.class).getReference("JKL-456");
      assertInstanceOf(HibernateProxy.class, ref);
    });
    assertEquals(List.of("select v1_0.id from Vehicle v1_0 where v1_0.licensePlate=?"), sql.statements());
  }

  @Test
  void findMultipleUsesOneInQuery() {
    List<Vehicle> vehicles = emf.callInTransaction(em -> em.unwrap(Session.class)
        .findMultiple(Vehicle.class, List.of("ABC-123", "JKL-456"), KeyType.NATURAL));
    assertEquals(List.of("Civic", "Model 3"), vehicles.stream().map(Vehicle::getModel).toList());
    assertEquals(List.of("select v1_0.id,v1_0.licensePlate,v1_0.model from Vehicle v1_0 where v1_0.licensePlate in (?,?)"),
        sql.statements());
  }

  @Test
  void changingAnImmutableNaturalIdFailsAtCommit() {
    RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em -> {
      Vehicle civic = em.find(Vehicle.class, "ABC-123", KeyType.NATURAL);
      civic.setLicensePlate("ABC-999");
    }));
    assertInstanceOf(HibernateException.class, e.getCause());
    assertEquals("An immutable natural identifier of entity com.howtodoinjava.hibernate.naturalid.Vehicle"
        + " was altered from `ABC-123` to `ABC-999`", e.getCause().getMessage());
    assertEquals("ABC-123", emf.callInTransaction(em ->
        em.find(Vehicle.class, "ABC-123", KeyType.NATURAL).getLicensePlate()));
  }

  @Test
  void mutableNaturalIdIsUpdatedAndSynchronizedBeforeFlush() {
    emf.runInTransaction(em -> em.persist(new Driver("L-100", "Lokesh")));
    reset();
    emf.runInTransaction(em -> {
      Driver lokesh = em.find(Driver.class, "L-100", KeyType.NATURAL);
      lokesh.setLicenseNumber("L-200");
      assertNull(em.find(Driver.class, "L-200", KeyType.NATURAL, NaturalIdSynchronization.DISABLED));
      assertSame(lokesh, em.find(Driver.class, "L-200", KeyType.NATURAL));
    });
    assertTrue(sql.statements().contains("update Driver set licenseNumber=?,name=? where id=?"));
    emf.runInTransaction(em -> {
      assertNull(em.find(Driver.class, "L-100", KeyType.NATURAL));
      assertEquals("Lokesh", em.find(Driver.class, "L-200", KeyType.NATURAL).getName());
    });
  }

  @Test
  @SuppressWarnings("deprecation")
  void compositeNaturalIdAcceptsNaturalIdClassMapAndUsing() {
    emf.runInTransaction(em -> {
      ParkingSpot a = em.find(ParkingSpot.class, new SpotKey("Downtown", 14), KeyType.NATURAL);
      ParkingSpot b = em.find(ParkingSpot.class, Map.of("garage", "Airport", "number", 14), KeyType.NATURAL);
      ParkingSpot c = em.unwrap(Session.class).byNaturalId(ParkingSpot.class)
          .using("garage", "Downtown")
          .using("number", 15)
          .load();
      assertTrue(a.isCovered());
      assertEquals("Airport", b.getGarage());
      assertEquals(15, c.getNumber());
    });
    // an array of the values, ordered alphabetically by attribute name: garage, number
    emf.runInTransaction(em -> assertEquals("Airport",
        em.find(ParkingSpot.class, new Object[] {"Airport", 14}, KeyType.NATURAL).getGarage()));
    assertEquals("select ps1_0.id,ps1_0.covered,ps1_0.garage,ps1_0.number from ParkingSpot ps1_0"
        + " where ps1_0.garage=? and ps1_0.number=?", sql.statements().getFirst());
  }

  @Test
  @SuppressWarnings("deprecation")
  void singleValueForCompositeNaturalIdFails() {
    HibernateException legacy = assertThrows(HibernateException.class, () -> emf.runInTransaction(em ->
        em.unwrap(Session.class).bySimpleNaturalId(ParkingSpot.class).load("Downtown")));
    assertEquals("Cannot interpret natural id value [Downtown] as compound natural id of entity"
        + " 'com.howtodoinjava.hibernate.naturalid.ParkingSpot'", legacy.getMessage());

    UnsupportedMappingException find = assertThrows(UnsupportedMappingException.class, () ->
        emf.runInTransaction(em -> em.find(ParkingSpot.class, "Downtown", KeyType.NATURAL)));
    assertEquals("Could not normalize compound natural id value: Downtown", find.getMessage());
  }

  @Test
  void duplicateNaturalIdViolatesTheUniqueConstraint() {
    RollbackException e = assertThrows(RollbackException.class, () ->
        emf.runInTransaction(em -> em.persist(new Vehicle("ABC-123", "Accord"))));
    assertInstanceOf(ConstraintViolationException.class, e.getCause());
    assertTrue(e.getMessage().contains("Unique index or primary key violation"));
  }

  @Test
  void compositeNaturalIdIsUniqueOnlyAsAPair() {
    // same number in another garage is fine
    emf.runInTransaction(em -> em.persist(new ParkingSpot("Airport", 15, true)));
    // same garage and number is rejected
    RollbackException e = assertThrows(RollbackException.class, () ->
        emf.runInTransaction(em -> em.persist(new ParkingSpot("Downtown", 14, false))));
    assertInstanceOf(ConstraintViolationException.class, e.getCause());
  }

  @Test
  void nullNaturalIdIsRejectedWhenTheColumnIsNotNull() {
    PropertyValueException e = assertThrows(PropertyValueException.class, () ->
        emf.runInTransaction(em -> em.persist(new Vehicle(null, "Accord"))));
    assertEquals("not-null property references a null or transient value for entity"
        + " com.howtodoinjava.hibernate.naturalid.Vehicle.licensePlate", e.getMessage());
    // Driver.licenseNumber has no @Column(nullable = false): a null natural id is stored
    emf.runInTransaction(em -> em.persist(new Driver(null, "Alex")));
  }

  @Test
  void jpqlQueryOnTheColumnRunsEveryTime() {
    emf.runInTransaction(em -> {
      for (int i = 0; i < 2; i++) {
        em.createQuery("from Vehicle where licensePlate = :plate", Vehicle.class)
            .setParameter("plate", "XYZ-789")
            .getSingleResult();
      }
    });
    assertEquals(2, Database.statements(emf));
  }

  @Test
  void equalsAndHashCodeOnTheNaturalIdSurvivePersist() {
    Vehicle prius = new Vehicle("PQR-321", "Prius");
    Set<Vehicle> fleet = new HashSet<>(Set.of(prius));
    emf.runInTransaction(em -> em.persist(prius));
    assertNotNull(prius.getId());
    assertTrue(fleet.contains(prius));
    Vehicle loaded = emf.callInTransaction(em -> em.find(Vehicle.class, "PQR-321", KeyType.NATURAL));
    assertTrue(fleet.contains(loaded));
  }

  @Test
  void secondLevelCacheResolvesNaturalIdWithoutSql() {
    SqlLog cachedSql = new SqlLog();
    try (EntityManagerFactory cached = Database.create(false, true, cachedSql)) {
      cached.runInTransaction(em -> {
        em.persist(new Vehicle("ABC-123", "Civic"));
        em.persist(new ParkingSpot("Downtown", 14, true));
      });
      Statistics stats = Database.statistics(cached);

      // Vehicle: @NaturalIdCache + @Cache
      stats.clear();
      cachedSql.clear();
      cached.runInTransaction(em -> assertEquals("Civic",
          em.find(Vehicle.class, "ABC-123", KeyType.NATURAL).getModel()));
      assertEquals(0, stats.getPrepareStatementCount());
      assertEquals(1, stats.getNaturalIdCacheHitCount());
      assertEquals(1, stats.getSecondLevelCacheHitCount());

      // ParkingSpot: @NaturalIdCache only, the entity is read by its id
      stats.clear();
      cached.runInTransaction(em -> assertTrue(
          em.find(ParkingSpot.class, new SpotKey("Downtown", 14), KeyType.NATURAL).isCovered()));
      assertEquals(1, stats.getPrepareStatementCount());
      assertEquals(1, stats.getNaturalIdCacheHitCount());
      assertEquals(List.of("select ps1_0.id,ps1_0.covered,ps1_0.garage,ps1_0.number from ParkingSpot ps1_0"
          + " where ps1_0.id=?"), cachedSql.statements());

      // an empty cache is filled by the first database read
      cached.getCache().evictAll();
      cached.unwrap(org.hibernate.SessionFactory.class).getCache().evictAllRegions();
      stats.clear();
      cached.runInTransaction(em -> em.find(Vehicle.class, "ABC-123", KeyType.NATURAL));
      assertEquals(1, stats.getPrepareStatementCount());
      stats.clear();
      cached.runInTransaction(em -> em.find(Vehicle.class, "ABC-123", KeyType.NATURAL));
      assertEquals(0, stats.getPrepareStatementCount());
    }
  }
}
