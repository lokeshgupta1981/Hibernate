package com.howtodoinjava.hibernate.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BatchProcessingTest {

  private static final Map<String, Object> NO_BATCHING = Map.of();
  private static final Map<String, Object> BATCH_50 = Map.of("hibernate.jdbc.batch_size", 50);
  private static final Map<String, Object> BATCH_50_ORDERED = Map.of(
      "hibernate.jdbc.batch_size", 50,
      "hibernate.order_inserts", true,
      "hibernate.order_updates", true);

  private static int dbNumber;

  private final JdbcCounter jdbc = new JdbcCounter();
  private EntityManagerFactory emf;

  private void open(Map<String, Object> settings) {
    emf = Database.create("jdbc:h2:mem:test" + (++dbNumber) + ";DB_CLOSE_DELAY=-1", jdbc, false, settings);
    jdbc.reset();
  }

  @AfterEach
  void tearDown() {
    if (emf != null) {
      emf.close();
    }
  }

  private static List<SensorReading> kitchen(int count) {
    return ReadingImport.readings("Kitchen", count);
  }

  private static List<String> sensorNames(int count) {
    return IntStream.rangeClosed(1, count).mapToObj(i -> "Sensor " + i).toList();
  }

  /** 1000 Kitchen readings plus 100 sensors with 3 readings each. */
  private void seed(Map<String, Object> settings) {
    open(settings);
    ReadingImport.importInChunks(emf, kitchen(1000), 50, 0);
    ReadingImport.importSensors(emf, sensorNames(100), 3);
    jdbc.reset();
  }

  // ------------------------------------------------------------------ inserts

  @Test
  void withoutBatchSizeEveryInsertIsOneRoundTrip() {
    open(NO_BATCHING);
    assertEquals(10_000, ReadingImport.importAll(emf, kitchen(10_000)));   // managed entities before commit
    assertEquals(10_000, jdbc.statements("insert"));
    assertEquals(10_000, jdbc.roundTrips("insert"));
    assertEquals(0, jdbc.batches("insert"));
  }

  @Test
  void batchSize50SendsTheInsertsIn200Batches() {
    open(BATCH_50);
    assertEquals(10_000, ReadingImport.importAll(emf, kitchen(10_000)));
    assertEquals(10_000, jdbc.statements("insert"));
    assertEquals(200, jdbc.roundTrips("insert"));
    assertEquals(Collections.nCopies(200, 50), jdbc.batchSizes("insert"));
    assertEquals(10_000, Database.count(emf, "SensorReading"));
  }

  @Test
  void fiveReadingsGoInOneBatchAfterTwoSequenceCalls() {
    open(BATCH_50);
    ReadingImport.importAll(emf, kitchen(5));
    List<JdbcCounter.Execution> calls = jdbc.executions();
    assertEquals(3, calls.size());
    assertEquals("select next value for reading_seq", calls.get(0).sql());
    assertEquals("select next value for reading_seq", calls.get(1).sql());
    assertTrue(calls.get(2).batch());
    assertEquals(5, calls.get(2).statements());
    assertEquals("insert into SensorReading (measuredAt,sensorName,temperature,id) values (?,?,?,?)",
        calls.get(2).sql());
  }

  @Test
  void pooledSequenceNeeds201CallsFor10000Ids() {
    open(BATCH_50);
    ReadingImport.importAll(emf, kitchen(10_000));
    assertEquals(201, jdbc.sequenceCalls());
  }

  @Test
  void sessionLevelBatchSizeWorksWithoutTheGlobalSetting() {
    open(NO_BATCHING);
    ReadingImport.importWithSessionBatchSize(emf, kitchen(10_000), 50);
    assertEquals(200, jdbc.batches("insert"));
    assertEquals(200, jdbc.roundTrips("insert"));
  }

  @Test
  void identityIdsDisableInsertBatching() {
    open(BATCH_50);
    assertEquals(10_000, ReadingImport.importLegacy(emf, "Kitchen", 10_000));
    assertEquals(10_000, jdbc.roundTrips("insert"));
    assertEquals(0, jdbc.batches("insert"));
    assertEquals(0, jdbc.sequenceCalls());
    assertTrue(jdbc.executions().get(0).sql().endsWith("values (?,?,?,default)"));
    assertEquals(10_000, Database.count(emf, "LegacyReading"));
  }

  // ------------------------------------------------------------------ flush() and clear()

  @Test
  void withoutClearThePersistenceContextHoldsEveryReading() {
    open(BATCH_50);
    List<Integer> managed = ReadingImport.importWithoutClear(emf, kitchen(100_000), 10_000);
    assertEquals(List.of(10_000, 20_000, 30_000, 40_000, 50_000, 60_000, 70_000, 80_000, 90_000, 100_000),
        managed);
  }

  @Test
  void flushAndClearKeepAtMost50ManagedReadings() {
    open(BATCH_50);
    List<Integer> managed = ReadingImport.importInChunks(emf, kitchen(10_000), 50, 1);
    assertEquals(50, managed.stream().mapToInt(Integer::intValue).max().orElseThrow());
    assertEquals(200, jdbc.batches("insert"));
    assertEquals(10_000, Database.count(emf, "SensorReading"));
  }

  // ------------------------------------------------------------------ order_inserts

  @Test
  void mixedEntityTypesBreakTheBatchesWithoutOrderInserts() {
    open(BATCH_50);
    ReadingImport.importSensors(emf, sensorNames(100), 3);
    assertEquals(400, jdbc.statements("insert"));
    assertEquals(200, jdbc.roundTrips("insert"));
    assertEquals(List.of(1, 3, 1, 3), jdbc.batchSizes("insert").subList(0, 4));
  }

  @Test
  void orderInsertsGroupsSensorsAndReadings() {
    open(BATCH_50_ORDERED);
    ReadingImport.importSensors(emf, sensorNames(100), 3);
    assertEquals(400, jdbc.statements("insert"));
    assertEquals(8, jdbc.roundTrips("insert"));
    assertEquals(Collections.nCopies(8, 50), jdbc.batchSizes("insert"));
    assertEquals(100, Database.count(emf, "Sensor"));
    assertEquals(300, Database.count(emf, "SensorReading"));
  }

  @Test
  void twoSensorsWithTwoReadingsEach() {
    open(BATCH_50);
    ReadingImport.importSensors(emf, List.of("Kitchen", "Garage"), 2);
    assertEquals(List.of(1, 2, 1, 2), jdbc.batchSizes("insert"));
    emf.close();

    open(BATCH_50_ORDERED);
    ReadingImport.importSensors(emf, List.of("Kitchen", "Garage"), 2);
    assertEquals(List.of(2, 4), jdbc.batchSizes("insert"));
  }

  // ------------------------------------------------------------------ updates and deletes

  @Test
  void updatesAreBatchedToo() {
    seed(BATCH_50);
    assertEquals(1000, ReadingImport.calibrate(emf, "Kitchen", -0.5));
    assertEquals(1000, jdbc.statements("update"));
    assertEquals(20, jdbc.batches("update"));
    double first = emf.callInTransaction(em -> em.createQuery(
        "select temperature from SensorReading where sensorName = 'Kitchen' order by measuredAt", Double.class)
        .setMaxResults(1).getSingleResult());
    assertEquals(19.5, first);
  }

  @Test
  void updatesWithoutBatchSizeAreOneRoundTripEach() {
    seed(NO_BATCHING);
    ReadingImport.calibrate(emf, "Kitchen", -0.5);
    assertEquals(1000, jdbc.roundTrips("update"));
    assertEquals(0, jdbc.batches("update"));
  }

  @Test
  void orderUpdatesGroupsSensorAndReadingUpdates() {
    seed(BATCH_50);
    ReadingImport.moveSensors(emf, sensorNames(100), "Office", FlushModeType.COMMIT);
    assertEquals(400, jdbc.statements("update"));
    assertEquals(200, jdbc.roundTrips("update"));
    emf.close();

    seed(BATCH_50_ORDERED);
    ReadingImport.moveSensors(emf, sensorNames(100), "Office", FlushModeType.COMMIT);
    assertEquals(400, jdbc.statements("update"));
    assertEquals(8, jdbc.roundTrips("update"));
  }

  @Test
  void autoFlushBeforeEachQueryDefeatsOrderUpdates() {
    seed(BATCH_50_ORDERED);
    ReadingImport.moveSensors(emf, sensorNames(100), "Office", FlushModeType.AUTO);
    assertEquals(200, jdbc.roundTrips("update"));
    assertEquals(List.of(1, 3, 1, 3), jdbc.batchSizes("update").subList(0, 4));
  }

  @Test
  void removeInALoopSendsBatchedDeletes() {
    seed(BATCH_50);
    assertEquals(1000, ReadingImport.deleteReadings(emf, "Kitchen"));
    assertEquals(1000, jdbc.statements("delete"));
    assertEquals(20, jdbc.batches("delete"));
    assertEquals(300, Database.count(emf, "SensorReading"));
  }

  @Test
  void jpqlBulkUpdateAndDeleteAreOneStatementEach() {
    seed(BATCH_50);
    assertEquals(1000, ReadingImport.bulkCalibrate(emf, "Kitchen", -0.5));
    assertEquals(1, jdbc.roundTrips("update"));
    assertEquals(1300, ReadingImport.bulkDeleteBefore(emf, ReadingImport.START.plusYears(1)));
    assertEquals(1, jdbc.roundTrips("delete"));
    assertEquals(0, Database.count(emf, "SensorReading"));
  }

  @Test
  void bulkUpdateDoesNotChangeLoadedEntities() {
    open(BATCH_50);
    ReadingImport.importAll(emf, kitchen(3));
    emf.runInTransaction(em -> {
      SensorReading first = em.createQuery("from SensorReading order by measuredAt", SensorReading.class)
          .setMaxResults(1).getSingleResult();
      assertEquals(20.0, first.getTemperature());
      em.createQuery("update SensorReading r set r.temperature = r.temperature - 0.5").executeUpdate();
      assertEquals(20.0, first.getTemperature());
      em.refresh(first);
      assertEquals(19.5, first.getTemperature());
    });
  }

  // ------------------------------------------------------------------ StatelessSession

  @Test
  void statelessSessionIgnoresTheBatchSizeSetting() {
    open(BATCH_50);
    ReadingImport.statelessInsert(emf, kitchen(10_000), null);
    assertEquals(10_000, jdbc.roundTrips("insert"));
    assertEquals(0, jdbc.batches("insert"));
  }

  @Test
  void statelessSessionBatchesAfterSetJdbcBatchSize() {
    open(BATCH_50);
    ReadingImport.statelessInsert(emf, kitchen(10_000), 50);
    assertEquals(10_000, jdbc.statements("insert"));
    assertEquals(201, jdbc.roundTrips("insert"));
    assertEquals(10_000, Database.count(emf, "SensorReading"));
  }

  @Test
  void statelessInsertMultipleBatches() {
    open(BATCH_50);
    ReadingImport.statelessInsertMultiple(emf, kitchen(10_000));
    assertEquals(10_000, jdbc.statements("insert"));
    assertEquals(201, jdbc.roundTrips("insert"));
  }

  @Test
  void statelessSessionDoesNotCascade() {
    open(BATCH_50);
    Sensor sensor = new Sensor("Kitchen", "Home");
    kitchen(2).forEach(sensor::addReading);
    emf.unwrap(SessionFactory.class).inStatelessTransaction(ss -> ss.insert(sensor));
    assertEquals(1, Database.count(emf, "Sensor"));
    assertEquals(0, Database.count(emf, "SensorReading"));
  }

  @Test
  void statelessSessionHasNoPersistenceContext() {
    open(BATCH_50);
    ReadingImport.importAll(emf, kitchen(1));
    emf.unwrap(SessionFactory.class).inStatelessTransaction(ss -> {
      Long id = ss.createSelectionQuery("select id from SensorReading", Long.class).getSingleResult();
      SensorReading a = ss.get(SensorReading.class, id);
      SensorReading b = ss.get(SensorReading.class, id);
      assertNotSame(a, b);
      a.setTemperature(30.0);   // no update() call
    });
    double stored = emf.callInTransaction(em ->
        em.createQuery("select temperature from SensorReading", Double.class).getSingleResult());
    assertEquals(20.0, stored);
  }

  // ------------------------------------------------------------------ settings

  @Test
  void hibernate7HasNoBatchVersionedDataSettingAndBatchesVersionedUpdates() {
    boolean settingExists = java.util.Arrays.stream(org.hibernate.cfg.BatchSettings.class.getFields())
        .anyMatch(f -> f.getName().equals("BATCH_VERSIONED_DATA"));
    assertFalse(settingExists);

    seed(BATCH_50_ORDERED);
    ReadingImport.moveSensors(emf, sensorNames(100), "Office", FlushModeType.COMMIT);
    assertEquals(List.of(50, 50), jdbc.batchSizes("update Sensor "));
    assertTrue(jdbc.executions().stream()
        .anyMatch(e -> e.sql().equals("update Sensor set location=?,name=?,version=? where id=? and version=?")));
  }

  @Test
  void orderingIsOffByDefault() {
    open(BATCH_50);
    var options = emf.unwrap(SessionFactoryImplementor.class).getSessionFactoryOptions();
    assertFalse(options.isOrderInsertsEnabled());
    assertFalse(options.isOrderUpdatesEnabled());
    assertEquals(50, options.getJdbcBatchSize());
  }

  @Test
  void hibernate7SessionHasNoSaveMethod() {
    assertFalse(java.util.Arrays.stream(org.hibernate.Session.class.getMethods())
        .anyMatch(m -> m.getName().equals("save") || m.getName().equals("saveOrUpdate")));
  }

  @Test
  void hibernate7HasNoBatchingBatchLoggerClass() {
    org.junit.jupiter.api.Assertions.assertThrows(ClassNotFoundException.class,
        () -> Class.forName("org.hibernate.engine.jdbc.batch.internal.BatchingBatch"));
  }

  @Test
  void identityInsertRunsAtPersist() {
    open(BATCH_50);
    emf.runInTransaction(em -> {
      LegacyReading reading = new LegacyReading("Kitchen", ReadingImport.START, 20.0);
      em.persist(reading);
      assertEquals(1, jdbc.roundTrips("insert"));     // before the flush
      assertTrue(reading.getId() != null);
    });
  }

  @Test
  void flushModeCommitQueriesFilterOnDatabaseValues() {
    seed(BATCH_50);
    emf.runInTransaction(em -> {
      em.setFlushMode(FlushModeType.COMMIT);
      Sensor sensor = em.createQuery("from Sensor where name = :name", Sensor.class)
          .setParameter("name", "Sensor 1").getSingleResult();
      sensor.setLocation("Office");
      long inOffice = em.createQuery("select count(*) from Sensor where location = 'Office'", Long.class)
          .getSingleResult();
      assertEquals(0, inOffice);
    });
  }

  @Test
  void changesToClearedReadingsAreNotSaved() {
    open(BATCH_50);
    SensorReading reading = kitchen(1).get(0);
    emf.runInTransaction(em -> {
      em.persist(reading);
      em.flush();
      em.clear();
      reading.setTemperature(99.0);
    });
    double stored = emf.callInTransaction(em ->
        em.createQuery("select temperature from SensorReading", Double.class).getSingleResult());
    assertEquals(20.0, stored);
  }
}
