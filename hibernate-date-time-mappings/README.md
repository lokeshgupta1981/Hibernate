# Hibernate Date and Time Mapping

Source code for the article [Hibernate Date and Time Mapping](https://howtodoinjava.com/hibernate/date-time-mappings/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the DDL, the stored values and the SQL for each step
mvn test                   # asserts every result shown in the article
```

The demo and the tests set the JVM default time zone to `America/New_York`, so the
`NORMALIZE` results are the same on every machine.

## Files

| File | What it shows |
|---|---|
| Webinar.java | `LocalDate`, `LocalTime`, `LocalDateTime`, `Instant`, `OffsetDateTime`, `ZonedDateTime`, `ZoneId`, `Duration`, `@CreationTimestamp`, `@UpdateTimestamp` |
| Broadcast.java | One `ZonedDateTime` stored with each `TimeZoneStorageType` (`NATIVE`, `NORMALIZE`, `NORMALIZE_UTC`, `COLUMN`) |
| WebinarSeries.java | `Year`, `YearMonth` (serialized and with a converter), `OffsetTime`, `Duration` as an interval, legacy `Date`/`Calendar` with `@Temporal`, `@CreationTimestamp(source = SourceType.DB)` |
| YearMonthConverter.java | `AttributeConverter` that stores a `YearMonth` in a `date` column |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) and reads column types and raw values |
| DateTimeDemo.java | Runs each step and prints the SQL and the stored values |
| DateTimeMappingTest.java | JUnit tests for every behavior described in the article |
