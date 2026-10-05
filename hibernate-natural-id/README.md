# Hibernate Natural Ids with @NaturalId

Source code for the article [Hibernate Natural Ids with @NaturalId](https://howtodoinjava.com/hibernate/hibernate-naturalid-example-tutorial/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- hibernate-jcache 7.4.11.Final with Ehcache 3.12.0 (second-level cache for `@NaturalIdCache`)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL and statement counts for each step
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Vehicle.java | Immutable natural id `licensePlate` with `@NaturalIdCache` and `@Cache`; `equals()`/`hashCode()` on the plate |
| Driver.java | Mutable natural id: `@NaturalId(mutable = true) licenseNumber` |
| ParkingSpot.java | Composite natural id (`garage` + `number`) with `@NaturalIdClass` and `@NaturalIdCache` only |
| SpotKey.java | Record used as the `@NaturalIdClass` for loading a parking spot |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`, second-level cache on or off, statistics |
| SqlLog.java | `StatementInspector` that records the SQL for the tests |
| NaturalIdDemo.java | Runs each case and prints the SQL and statement counts |
| NaturalIdTest.java | JUnit tests for every behavior described in the article |
