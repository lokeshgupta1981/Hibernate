package com.howtodoinjava.hibernate.naturalid;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import org.hibernate.KeyType;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;

/**
 * Runs every case of the article and prints the SQL Hibernate sends.
 * Sections 1 to 8 use a factory without the second-level cache; section 9 turns it on.
 */
public class NaturalIdDemo {

  public static void main(String[] args) {
    title("1. Schema: Hibernate adds a unique constraint for each natural id");
    try (EntityManagerFactory emf = Database.create(true, false)) {
      withoutCache(emf);
    }
    title("9. Second-level cache on (@NaturalIdCache + @Cache)");
    try (EntityManagerFactory emf = Database.create(true, true)) {
      withCache(emf);
    }
  }

  static void withoutCache(EntityManagerFactory emf) {
    title("2. Insert three vehicles");
    emf.runInTransaction(em -> {
      em.persist(new Vehicle("ABC-123", "Civic"));
      em.persist(new Vehicle("XYZ-789", "Corolla"));
      em.persist(new Vehicle("JKL-456", "Model 3"));
    });

    title("3. find(Vehicle.class, plate, KeyType.NATURAL), twice, then by id, in one EntityManager");
    Database.resetStatistics(emf);
    emf.runInTransaction(em -> {
      Vehicle civic = em.find(Vehicle.class, "ABC-123", KeyType.NATURAL);
      Vehicle again = em.find(Vehicle.class, "ABC-123", KeyType.NATURAL);
      Vehicle byId = em.find(Vehicle.class, civic.getId());
      System.out.println("found " + civic + ", same instance: " + (civic == again && civic == byId));
    });
    System.out.println("statements: " + Database.statements(emf));

    title("3b. The same plate in a new EntityManager");
    Database.resetStatistics(emf);
    emf.runInTransaction(em -> em.find(Vehicle.class, "ABC-123", KeyType.NATURAL));
    System.out.println("statements: " + Database.statements(emf));

    title("3c. Unknown plate");
    emf.runInTransaction(em ->
        System.out.println("result: " + em.find(Vehicle.class, "NOP-000", KeyType.NATURAL)));

    title("4. Legacy API: bySimpleNaturalId() (deprecated in Hibernate 7)");
    emf.runInTransaction(em -> {
      Session session = em.unwrap(Session.class);
      Vehicle corolla = session.bySimpleNaturalId(Vehicle.class).load("XYZ-789");
      Vehicle missing = session.bySimpleNaturalId(Vehicle.class).load("NOP-000");
      System.out.println("load: " + corolla + ", missing: " + missing);
    });
    Database.resetStatistics(emf);
    emf.runInTransaction(em -> {
      Vehicle ref = em.unwrap(Session.class).bySimpleNaturalId(Vehicle.class).getReference("JKL-456");
      System.out.println("getReference: " + ref.getClass().getSimpleName());
    });
    System.out.println("statements: " + Database.statements(emf));

    title("5. Several plates at once with findMultiple()");
    emf.runInTransaction(em -> {
      List<Vehicle> vehicles = em.unwrap(Session.class)
          .findMultiple(Vehicle.class, List.of("ABC-123", "JKL-456"), KeyType.NATURAL);
      System.out.println("result: " + vehicles);
    });

    title("6. Change an immutable natural id");
    try {
      emf.runInTransaction(em -> {
        Vehicle civic = em.find(Vehicle.class, "ABC-123", KeyType.NATURAL);
        civic.setLicensePlate("ABC-999");
      });
    } catch (RuntimeException e) {
      printError(e);
    }

    title("6b. Mutable natural id: the driver gets a new license number");
    emf.runInTransaction(em -> em.persist(new Driver("L-100", "Lokesh")));
    emf.runInTransaction(em -> {
      Driver lokesh = em.find(Driver.class, "L-100", KeyType.NATURAL);
      lokesh.setLicenseNumber("L-200");
      Driver noSync = em.find(Driver.class, "L-200", KeyType.NATURAL, NaturalIdSynchronization.DISABLED);
      Driver withSync = em.find(Driver.class, "L-200", KeyType.NATURAL);
      System.out.println("sync disabled: " + noSync + ", default: " + withSync);
    });
    emf.runInTransaction(em -> System.out.println("after commit, old: "
        + em.find(Driver.class, "L-100", KeyType.NATURAL)
        + ", new: " + em.find(Driver.class, "L-200", KeyType.NATURAL)));

    title("7. Composite natural id: garage + number");
    emf.runInTransaction(em -> {
      em.persist(new ParkingSpot("Downtown", 14, true));
      em.persist(new ParkingSpot("Downtown", 15, false));
      em.persist(new ParkingSpot("Airport", 14, false));
    });
    emf.runInTransaction(em -> {
      ParkingSpot a = em.find(ParkingSpot.class, new SpotKey("Downtown", 14), KeyType.NATURAL);
      ParkingSpot b = em.find(ParkingSpot.class, Map.of("garage", "Airport", "number", 14), KeyType.NATURAL);
      ParkingSpot c = em.unwrap(Session.class).byNaturalId(ParkingSpot.class)
          .using("garage", "Downtown")
          .using("number", 15)
          .load();
      System.out.println("SpotKey: " + a + ", Map: " + b + ", byNaturalId(): " + c);
    });
    try {
      emf.runInTransaction(em -> em.unwrap(Session.class).bySimpleNaturalId(ParkingSpot.class).load("Downtown"));
    } catch (RuntimeException e) {
      printError(e);
    }
    try {
      emf.runInTransaction(em -> em.find(ParkingSpot.class, "Downtown", KeyType.NATURAL));
    } catch (RuntimeException e) {
      printError(e);
    }

    title("8. Duplicate plate and null plate");
    try {
      emf.runInTransaction(em -> em.persist(new Vehicle("ABC-123", "Accord")));
    } catch (RuntimeException e) {
      printError(e);
    }
    try {
      emf.runInTransaction(em -> em.persist(new Vehicle(null, "Accord")));
    } catch (RuntimeException e) {
      printError(e);
    }

    title("8b. JPQL query on the plate, twice in one EntityManager");
    Database.resetStatistics(emf);
    emf.runInTransaction(em -> {
      for (int i = 0; i < 2; i++) {
        em.createQuery("from Vehicle where licensePlate = :plate", Vehicle.class)
            .setParameter("plate", "XYZ-789")
            .getSingleResult();
      }
    });
    System.out.println("statements: " + Database.statements(emf));
  }

  static void withCache(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      em.persist(new Vehicle("ABC-123", "Civic"));
      em.persist(new ParkingSpot("Downtown", 14, true));
    });

    title("9a. Vehicle (@NaturalIdCache + @Cache) in a new EntityManager");
    Database.resetStatistics(emf);
    emf.runInTransaction(em -> em.find(Vehicle.class, "ABC-123", KeyType.NATURAL));
    printCacheStats(emf);

    title("9b. ParkingSpot (@NaturalIdCache only) in a new EntityManager");
    Database.resetStatistics(emf);
    emf.runInTransaction(em -> em.find(ParkingSpot.class, new SpotKey("Downtown", 14), KeyType.NATURAL));
    printCacheStats(emf);
  }

  static void printCacheStats(EntityManagerFactory emf) {
    Statistics stats = Database.statistics(emf);
    System.out.println("statements: " + stats.getPrepareStatementCount()
        + ", natural id cache hits: " + stats.getNaturalIdCacheHitCount()
        + ", entity cache hits: " + stats.getSecondLevelCacheHitCount());
  }

  static void printError(Throwable e) {
    Throwable root = e;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    System.out.println(e.getClass().getName() + ": " + e.getMessage());
    if (root != e) {
      System.out.println("  root cause " + root.getClass().getName() + ": " + root.getMessage());
    }
  }

  static void title(String text) {
    System.out.println();
    System.out.println("== " + text + " ==");
  }
}
