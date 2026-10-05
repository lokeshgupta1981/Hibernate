# Batch Processing with Hibernate/JPA

Source code for the article [Batch Processing with Hibernate/JPA](https://howtodoinjava.com/hibernate/batch-processing/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database, and a TCP server on localhost for the timings)
- datasource-proxy 1.11.0 (counts the JDBC calls)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the JDBC calls for each case, the counts, memory and timings (about 2 minutes)
mvn test                   # asserts every count and result shown in the article
```

The timings depend on the machine and change between runs. Treat them as a rough comparison only.

## Files

| File | What it shows |
|---|---|
| SensorReading.java | Entity with a `SEQUENCE` id (`allocationSize = 50`), the id strategy that allows insert batching |
| LegacyReading.java | Same columns with an `IDENTITY` id, which disables insert batching |
| Sensor.java | Second entity with `@Version` and a cascading `@OneToMany`, for `order_inserts` and `order_updates` |
| JdbcCounter.java | datasource-proxy listener that counts round trips, batches and statements |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` on a proxied H2 `DataSource` |
| ReadingImport.java | The import, update, delete, bulk JPQL and `StatelessSession` jobs |
| BatchProcessingDemo.java | Runs each job and prints the JDBC calls, counts, memory and timings |
| BatchProcessingTest.java | JUnit tests for every behavior described in the article |
