package com.howtodoinjava.hibernate.batch;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.h2.tools.Server;
import org.hibernate.SessionFactory;

public class BatchProcessingDemo {

  static final Map<String, Object> NO_BATCHING = Map.of();
  static final Map<String, Object> BATCH_50 = Map.of("hibernate.jdbc.batch_size", 50);
  static final Map<String, Object> BATCH_50_ORDERED = Map.of(
      "hibernate.jdbc.batch_size", 50,
      "hibernate.order_inserts", true,
      "hibernate.order_updates", true);

  static final JdbcCounter COUNTER = new JdbcCounter();
  static int dbNumber;

  public static void main(String[] args) throws Exception {
    if (args.length > 0 && args[0].equals("counts")) {
      countRuns(10_000);
      return;
    }
    smallRuns();
    countRuns(10_000);
    memoryRun(100_000);
    timingRuns(100_000);
  }

  // ---------------------------------------------------------------- SQL for a few rows

  static void smallRuns() {
    title("1. Five readings, batching OFF (default)");
    small(NO_BATCHING, emf -> ReadingImport.importAll(emf, ReadingImport.readings("Kitchen", 5)));

    title("2. Five readings, hibernate.jdbc.batch_size = 50");
    small(BATCH_50, emf -> ReadingImport.importAll(emf, ReadingImport.readings("Kitchen", 5)));

    title("3. Five LegacyReading rows (IDENTITY), hibernate.jdbc.batch_size = 50");
    small(BATCH_50, emf -> ReadingImport.importLegacy(emf, "Kitchen", 5));

    title("4. Two sensors with two readings each, batch_size = 50, order_inserts = false");
    small(BATCH_50, emf -> ReadingImport.importSensors(emf, List.of("Kitchen", "Garage"), 2));

    title("5. Same, order_inserts = true");
    small(BATCH_50_ORDERED, emf -> ReadingImport.importSensors(emf, List.of("Kitchen", "Garage"), 2));

    title("6. Calibrate (update) three readings, batch_size = 50");
    smallAfterImport(BATCH_50, 3, emf -> ReadingImport.calibrate(emf, "Kitchen", -0.5));

    title("7. Delete three readings with remove(), batch_size = 50");
    smallAfterImport(BATCH_50, 3, emf -> ReadingImport.deleteReadings(emf, "Kitchen"));

    title("8. JPQL bulk update and bulk delete");
    smallAfterImport(BATCH_50, 3, emf -> {
      int updated = ReadingImport.bulkCalibrate(emf, "Kitchen", -0.5);
      int deleted = ReadingImport.bulkDeleteBefore(emf, ReadingImport.START.plusMinutes(2));
      System.out.println("updated = " + updated + ", deleted = " + deleted);
    });

    title("8b. Bulk update and an entity that is already loaded");
    smallAfterImport(BATCH_50, 3, emf -> emf.runInTransaction(em -> {
      SensorReading first = em.createQuery("from SensorReading order by measuredAt", SensorReading.class)
          .setMaxResults(1).getSingleResult();
      System.out.println("loaded: " + first.getTemperature());
      em.createQuery("update SensorReading r set r.temperature = r.temperature - 0.5").executeUpdate();
      System.out.println("after bulk update, same object: " + first.getTemperature());
      em.refresh(first);
      System.out.println("after refresh(): " + first.getTemperature());
    }));

    title("9. StatelessSession.insert(), batch_size = 50 in the settings");
    small(BATCH_50, emf -> ReadingImport.statelessInsert(emf, ReadingImport.readings("Kitchen", 3), null));

    title("10. StatelessSession.insert() after setJdbcBatchSize(50)");
    small(BATCH_50, emf -> ReadingImport.statelessInsert(emf, ReadingImport.readings("Kitchen", 3), 50));

    title("11. StatelessSession.insertMultiple()");
    small(BATCH_50, emf -> ReadingImport.statelessInsertMultiple(emf, ReadingImport.readings("Kitchen", 3)));

    title("12. StatelessSession.insert(sensor) with two readings in its list");
    small(BATCH_50, emf -> {
      Sensor sensor = new Sensor("Kitchen", "Home");
      ReadingImport.readings("Kitchen", 2).forEach(sensor::addReading);
      emf.unwrap(SessionFactory.class).inStatelessTransaction(ss -> ss.insert(sensor));
      System.out.println("sensors = " + Database.count(emf, "Sensor")
          + ", readings = " + Database.count(emf, "SensorReading"));
    });

    title("12b. StatelessSession has no persistence context");
    smallAfterImport(BATCH_50, 1, emf -> emf.unwrap(SessionFactory.class).inStatelessTransaction(ss -> {
      Long id = ss.createSelectionQuery("select id from SensorReading", Long.class).getSingleResult();
      SensorReading a = ss.get(SensorReading.class, id);
      SensorReading b = ss.get(SensorReading.class, id);
      System.out.println("same instance: " + (a == b));
      a.setTemperature(30.0);
      System.out.println("changed in memory, update() not called");
    }));
    System.out.println("(the row keeps its value: no dirty checking)");

    title("13. Session.setJdbcBatchSize(50), no hibernate.jdbc.batch_size setting");
    small(NO_BATCHING, emf -> ReadingImport.importWithSessionBatchSize(emf, ReadingImport.readings("Kitchen", 5), 50));
  }

  static void small(Map<String, Object> settings, Consumer<EntityManagerFactory> work) {
    EntityManagerFactory emf = Database.create(nextUrl(), COUNTER, false, settings);
    COUNTER.reset();
    work.accept(emf);
    printExecutions();
    emf.close();
  }

  static void smallAfterImport(Map<String, Object> settings, int readings, Consumer<EntityManagerFactory> work) {
    EntityManagerFactory emf = Database.create(nextUrl(), COUNTER, false, settings);
    ReadingImport.importAll(emf, ReadingImport.readings("Kitchen", readings));
    COUNTER.reset();
    work.accept(emf);
    printExecutions();
    emf.close();
  }

  static void printExecutions() {
    System.out.println("-- JDBC calls (datasource-proxy):");
    for (JdbcCounter.Execution e : COUNTER.executions()) {
      String sql = e.sql();
      System.out.println("   " + (e.batch() ? "executeBatch(" + e.statements() + ")" : "execute") + "  " + sql);
    }
  }

  // ---------------------------------------------------------------- counts for many rows

  static void countRuns(int n) {
    title("Counts for " + n + " readings");
    count("A. persist() loop, batching off", NO_BATCHING,
        emf -> ReadingImport.importAll(emf, ReadingImport.readings("Kitchen", n)), "insert");
    count("B. persist() loop, batch_size 50", BATCH_50,
        emf -> ReadingImport.importAll(emf, ReadingImport.readings("Kitchen", n)), "insert");
    count("C. persist() + flush()/clear() every 50, batch_size 50", BATCH_50,
        emf -> ReadingImport.importInChunks(emf, ReadingImport.readings("Kitchen", n), 50, 0), "insert");
    count("D. IDENTITY (LegacyReading), batch_size 50", BATCH_50,
        emf -> ReadingImport.importLegacy(emf, "Kitchen", n), "insert");
    count("B2. persist() loop, session.setJdbcBatchSize(50), no global setting", NO_BATCHING,
        emf -> ReadingImport.importWithSessionBatchSize(emf, ReadingImport.readings("Kitchen", n), 50), "insert");
    count("E. StatelessSession.insert(), batch_size 50 setting only", BATCH_50,
        emf -> ReadingImport.statelessInsert(emf, ReadingImport.readings("Kitchen", n), null), "insert");
    count("F. StatelessSession.insert() + setJdbcBatchSize(50)", BATCH_50,
        emf -> ReadingImport.statelessInsert(emf, ReadingImport.readings("Kitchen", n), 50), "insert");
    count("G. StatelessSession.insertMultiple()", BATCH_50,
        emf -> ReadingImport.statelessInsertMultiple(emf, ReadingImport.readings("Kitchen", n)), "insert");

    List<String> sensors = List.of("Kitchen", "Garage", "Garden", "Attic", "Porch");
    count("H. 5 sensors x 200 readings (cascade), batch_size 50, no ordering", BATCH_50,
        emf -> ReadingImport.importSensors(emf, sensors, 200), "insert");
    count("I. 5 sensors x 200 readings (cascade), batch_size 50, order_inserts", BATCH_50_ORDERED,
        emf -> ReadingImport.importSensors(emf, sensors, 200), "insert");
    count("H2. 100 sensors x 3 readings (cascade), batch_size 50, no ordering", BATCH_50,
        emf -> ReadingImport.importSensors(emf, names(100), 3), "insert");
    count("I2. 100 sensors x 3 readings (cascade), batch_size 50, order_inserts", BATCH_50_ORDERED,
        emf -> ReadingImport.importSensors(emf, names(100), 3), "insert");

    countAfterSensors("J. calibrate 1000 readings, batch_size 50", BATCH_50,
        emf -> ReadingImport.calibrate(emf, "Kitchen", -0.5), "update");
    countAfterSensors("K. move 100 sensors, flush mode COMMIT, no ordering", BATCH_50,
        emf -> ReadingImport.moveSensors(emf, names(100), "Office", FlushModeType.COMMIT), "update");
    countAfterSensors("L. move 100 sensors, flush mode COMMIT, order_updates", BATCH_50_ORDERED,
        emf -> ReadingImport.moveSensors(emf, names(100), "Office", FlushModeType.COMMIT), "update");
    countAfterSensors("K2. move 100 sensors, flush mode AUTO, no ordering", BATCH_50,
        emf -> ReadingImport.moveSensors(emf, names(100), "Office", FlushModeType.AUTO), "update");
    countAfterSensors("L2. move 100 sensors, flush mode AUTO, order_updates", BATCH_50_ORDERED,
        emf -> ReadingImport.moveSensors(emf, names(100), "Office", FlushModeType.AUTO), "update");
    countAfterSensors("M. calibrate 1000 readings, batching off", NO_BATCHING,
        emf -> ReadingImport.calibrate(emf, "Kitchen", -0.5), "update");
    countAfterSensors("N. delete 1000 readings with remove(), batch_size 50", BATCH_50,
        emf -> ReadingImport.deleteReadings(emf, "Kitchen"), "delete");
    countAfterSensors("O. JPQL bulk update", BATCH_50,
        emf -> ReadingImport.bulkCalibrate(emf, "Kitchen", -0.5), "update");
    countAfterSensors("P. JPQL bulk delete", BATCH_50,
        emf -> ReadingImport.bulkDeleteBefore(emf, LocalDateTime.of(2030, 1, 1, 0, 0)), "delete");
  }

  static List<String> names(int n) {
    return java.util.stream.IntStream.rangeClosed(1, n).mapToObj(i -> "Sensor " + i).toList();
  }

  static void count(String label, Map<String, Object> settings, Consumer<EntityManagerFactory> work, String kind) {
    EntityManagerFactory emf = Database.create(nextUrl(), COUNTER, false, settings);
    COUNTER.reset();
    work.accept(emf);
    printCounts(label, kind);
    emf.close();
  }

  /** Seeds Kitchen with 1000 readings and 100 sensors with 3 readings each, then runs the work. */
  static void countAfterSensors(String label, Map<String, Object> settings, Consumer<EntityManagerFactory> work,
      String kind) {
    EntityManagerFactory emf = Database.create(nextUrl(), COUNTER, false, settings);
    ReadingImport.importInChunks(emf, ReadingImport.readings("Kitchen", 1000), 50, 0);
    ReadingImport.importSensors(emf, names(100), 3);
    COUNTER.reset();
    work.accept(emf);
    printCounts(label, kind);
    emf.close();
  }

  static void printCounts(String label, String kind) {
    List<Integer> sizes = COUNTER.batchSizes(kind);
    System.out.printf("%-70s %s statements=%d roundTrips=%d batches=%d sequenceCalls=%d allCalls=%d sizes=%s%n",
        label, kind, COUNTER.statements(kind), COUNTER.roundTrips(kind), COUNTER.batches(kind),
        COUNTER.sequenceCalls(), COUNTER.total(),
        sizes.size() > 8 ? sizes.subList(0, 8) + "... last=" + sizes.get(sizes.size() - 1) : sizes);
  }

  // ---------------------------------------------------------------- memory

  static void memoryRun(int n) {
    title("Managed entities and heap for " + n + " readings, batch_size 50 (sandbox measurement)");
    EntityManagerFactory emf = Database.create(nextUrl(), COUNTER, false, BATCH_50);
    List<Integer> without = ReadingImport.importWithoutClear(emf, ReadingImport.readings("Kitchen", n), n / 10);
    System.out.println("without clear(): managed entities every " + n / 10 + " readings = " + without);
    emf.close();

    emf = Database.create(nextUrl(), COUNTER, false, BATCH_50);
    List<Integer> with = ReadingImport.importInChunks(emf, ReadingImport.readings("Kitchen", n), 50, n / 10);
    System.out.println("with flush()+clear() every 50: managed entities every " + n / 10 + " readings = " + with);
    emf.close();

    System.out.println("heap without clear(): " + heapAtEnd(n, false) + " MB");
    System.out.println("heap with clear():    " + heapAtEnd(n, true) + " MB");
  }

  /** Used heap (after GC) at the end of the loop, before commit. */
  static long heapAtEnd(int n, boolean clear) {
    EntityManagerFactory emf = Database.create(nextUrl(), COUNTER, false, BATCH_50);
    gc();
    long before = usedMb();
    long[] atEnd = new long[1];
    emf.runInTransaction(em -> {
      for (int i = 0; i < n; i++) {
        em.persist(new SensorReading("Kitchen", ReadingImport.START.plusMinutes(i), 20.0 + (i % 10) / 2.0));
        if (clear && (i + 1) % 50 == 0) {
          em.flush();
          em.clear();
        }
      }
      gc();
      atEnd[0] = usedMb();
    });
    emf.close();
    return atEnd[0] - before;
  }

  static void gc() {
    for (int i = 0; i < 3; i++) {
      System.gc();
    }
  }

  static long usedMb() {
    Runtime rt = Runtime.getRuntime();
    return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
  }

  // ---------------------------------------------------------------- timings

  static void timingRuns(int n) throws Exception {
    Server server = Server.createTcpServer("-tcpPort", "0", "-ifNotExists").start();
    try {
      String base = "jdbc:h2:tcp://localhost:" + server.getPort() + "/mem:timing";
      title("Timings for " + n + " readings, H2 over TCP on localhost (sandbox measurement, best of 3)");
      for (int round = 0; round < 2; round++) {   // round 0 warms up the JVM with fewer rows
        boolean print = round == 1;
        int rows = print ? n : n / 10;
        timeAll(print, base, rows);
      }
    }
    finally {
      server.stop();
    }
  }

  static void timeAll(boolean print, String base, int n) {
    time(print, base, "persist(), batching off", NO_BATCHING,
        emf -> ReadingImport.importAll(emf, ReadingImport.readings("Kitchen", n)));
    time(print, base, "persist(), IDENTITY, batch_size 50", BATCH_50,
        emf -> ReadingImport.importLegacy(emf, "Kitchen", n));
    time(print, base, "persist(), batch_size 50", BATCH_50,
        emf -> ReadingImport.importAll(emf, ReadingImport.readings("Kitchen", n)));
    time(print, base, "persist() + flush()/clear() every 50, batch_size 50", BATCH_50,
        emf -> ReadingImport.importInChunks(emf, ReadingImport.readings("Kitchen", n), 50, 0));
    time(print, base, "StatelessSession.insert(), no setJdbcBatchSize()", BATCH_50,
        emf -> ReadingImport.statelessInsert(emf, ReadingImport.readings("Kitchen", n), null));
    time(print, base, "StatelessSession.insert(), setJdbcBatchSize(50)", BATCH_50,
        emf -> ReadingImport.statelessInsert(emf, ReadingImport.readings("Kitchen", n), 50));
    time(print, base, "StatelessSession.insertMultiple()", BATCH_50,
        emf -> ReadingImport.statelessInsertMultiple(emf, ReadingImport.readings("Kitchen", n)));
  }

  static void time(boolean print, String base, String label, Map<String, Object> settings,
      Consumer<EntityManagerFactory> work) {
    long best = Long.MAX_VALUE;
    for (int i = 0; i < 3; i++) {
      EntityManagerFactory emf = Database.create(base + (++dbNumber) + ";DB_CLOSE_DELAY=-1", COUNTER, false, settings);
      COUNTER.reset();
      long start = System.nanoTime();
      work.accept(emf);
      best = Math.min(best, (System.nanoTime() - start) / 1_000_000);
      emf.close();
    }
    if (print) {
      System.out.printf("%-55s %6d ms   (JDBC calls: %d)%n", label, best, COUNTER.total());
    }
  }

  // ---------------------------------------------------------------- helpers

  static String nextUrl() {
    dbNumber++;
    return currentUrl();
  }

  static String currentUrl() {
    return "jdbc:h2:mem:sensors" + dbNumber + ";DB_CLOSE_DELAY=-1";
  }

  static void title(String text) {
    System.out.println();
    System.out.println("=== " + text);
  }
}
