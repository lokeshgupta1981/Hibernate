package com.howtodoinjava.hibernate.batch;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/** The import jobs used by the demo and the tests. */
public final class ReadingImport {

  public static final LocalDateTime START = LocalDateTime.of(2026, 10, 1, 0, 0);

  private ReadingImport() {
  }

  /** One reading per minute: 20.0, 20.5, 21.0 ... 24.5, then again from 20.0. */
  public static List<SensorReading> readings(String sensorName, int count) {
    List<SensorReading> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      list.add(new SensorReading(sensorName, START.plusMinutes(i), 20.0 + (i % 10) / 2.0));
    }
    return list;
  }

  /** Number of entities the persistence context of this EntityManager holds right now. */
  public static int managedEntities(EntityManager em) {
    return em.unwrap(Session.class).getStatistics().getEntityCount();
  }

  /** persist() in a loop, one transaction, no flush() or clear(). Returns the managed count before commit. */
  public static int importAll(EntityManagerFactory emf, List<SensorReading> readings) {
    return emf.callInTransaction(em -> {
      for (SensorReading reading : readings) {
        em.persist(reading);
      }
      return managedEntities(em);
    });
  }

  /**
   * persist() in a loop with flush() and clear() every batchSize readings.
   * Returns the managed entity count sampled after every sampleEvery readings.
   */
  public static List<Integer> importInChunks(EntityManagerFactory emf, List<SensorReading> readings,
      int batchSize, int sampleEvery) {
    return emf.callInTransaction(em -> {
      List<Integer> samples = new ArrayList<>();
      for (int i = 0; i < readings.size(); i++) {
        em.persist(readings.get(i));
        if (sampleEvery > 0 && (i + 1) % sampleEvery == 0) {
          samples.add(managedEntities(em));
        }
        if ((i + 1) % batchSize == 0) {
          em.flush();
          em.clear();
        }
      }
      return samples;
    });
  }

  /** Same loop as importAll(), without clear(), sampling the managed count. */
  public static List<Integer> importWithoutClear(EntityManagerFactory emf, List<SensorReading> readings,
      int sampleEvery) {
    return emf.callInTransaction(em -> {
      List<Integer> samples = new ArrayList<>();
      for (int i = 0; i < readings.size(); i++) {
        em.persist(readings.get(i));
        if ((i + 1) % sampleEvery == 0) {
          samples.add(managedEntities(em));
        }
      }
      return samples;
    });
  }

  /** Batching switched on for one EntityManager only, with Session.setJdbcBatchSize(). */
  public static void importWithSessionBatchSize(EntityManagerFactory emf, List<SensorReading> readings,
      int batchSize) {
    emf.runInTransaction(em -> {
      em.unwrap(Session.class).setJdbcBatchSize(batchSize);
      readings.forEach(em::persist);
    });
  }

  /** IDENTITY ids: the same loop for the LegacyReading table. Returns the managed count before commit. */
  public static int importLegacy(EntityManagerFactory emf, String sensorName, int count) {
    return emf.callInTransaction(em -> {
      for (int i = 0; i < count; i++) {
        em.persist(new LegacyReading(sensorName, START.plusMinutes(i), 20.0 + (i % 10) / 2.0));
      }
      return managedEntities(em);
    });
  }

  /** Sensors with their readings, persisted through the cascade: Sensor, readings, Sensor, readings ... */
  public static void importSensors(EntityManagerFactory emf, List<String> names, int readingsPerSensor) {
    emf.runInTransaction(em -> {
      for (String name : names) {
        Sensor sensor = new Sensor(name, "Home");
        readings(name, readingsPerSensor).forEach(sensor::addReading);
        em.persist(sensor);
      }
    });
  }

  /** Loads the readings of one sensor and corrects each temperature: one UPDATE per reading. */
  public static int calibrate(EntityManagerFactory emf, String sensorName, double offset) {
    return emf.callInTransaction(em -> {
      List<SensorReading> list = em.createQuery(
              "from SensorReading where sensorName = :name", SensorReading.class)
          .setParameter("name", sensorName)
          .getResultList();
      list.forEach(r -> r.setTemperature(r.getTemperature() + offset));
      return list.size();
    });
  }

  /**
   * Changes each sensor and its readings, so UPDATE statements of both tables alternate.
   * With FlushModeType.AUTO the query in the loop flushes the previous changes first.
   */
  public static void moveSensors(EntityManagerFactory emf, List<String> names, String location,
      FlushModeType flushMode) {
    emf.runInTransaction(em -> {
      em.setFlushMode(flushMode);
      for (String name : names) {
        Sensor sensor = em.createQuery("from Sensor where name = :name", Sensor.class)
            .setParameter("name", name)
            .getSingleResult();
        sensor.setLocation(location);
        em.createQuery("from SensorReading where sensorName = :name", SensorReading.class)
            .setParameter("name", name)
            .getResultList()
            .forEach(r -> r.setTemperature(r.getTemperature() - 0.5));
      }
    });
  }

  /** Loads the readings of one sensor and removes them one by one: one DELETE per reading. */
  public static int deleteReadings(EntityManagerFactory emf, String sensorName) {
    return emf.callInTransaction(em -> {
      List<SensorReading> list = em.createQuery(
              "from SensorReading where sensorName = :name", SensorReading.class)
          .setParameter("name", sensorName)
          .getResultList();
      list.forEach(em::remove);
      return list.size();
    });
  }

  /** One JPQL UPDATE statement for all readings of a sensor. */
  public static int bulkCalibrate(EntityManagerFactory emf, String sensorName, double offset) {
    return emf.callInTransaction(em -> em.createQuery(
            "update SensorReading r set r.temperature = r.temperature + :offset where r.sensorName = :name")
        .setParameter("offset", offset)
        .setParameter("name", sensorName)
        .executeUpdate());
  }

  /** One JPQL DELETE statement for all readings older than the given time. */
  public static int bulkDeleteBefore(EntityManagerFactory emf, LocalDateTime before) {
    return emf.callInTransaction(em -> em.createQuery(
            "delete from SensorReading r where r.measuredAt < :before")
        .setParameter("before", before)
        .executeUpdate());
  }

  /** StatelessSession.insert() in a loop. batchSize null means: do not call setJdbcBatchSize(). */
  public static void statelessInsert(EntityManagerFactory emf, List<SensorReading> readings, Integer batchSize) {
    emf.unwrap(SessionFactory.class).inStatelessTransaction(ss -> {
      if (batchSize != null) {
        ss.setJdbcBatchSize(batchSize);
      }
      for (SensorReading reading : readings) {
        ss.insert(reading);
      }
    });
  }

  /** StatelessSession.insertMultiple(): one call for the whole list. */
  public static void statelessInsertMultiple(EntityManagerFactory emf, List<SensorReading> readings) {
    emf.unwrap(SessionFactory.class).inStatelessTransaction(ss -> ss.insertMultiple(readings));
  }
}
