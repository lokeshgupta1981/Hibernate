# Configure c3p0 with Hibernate and Spring

Source code for the article [Configure c3p0 with Hibernate and Spring](https://howtodoinjava.com/hibernate/hibernate-c3p0-connection-pool-configuration-tutorial/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final with hibernate-c3p0 7.4.11.Final
- c3p0 0.14.2
- H2 2.5.252 (in-memory and file databases)
- JUnit 6.1.3
- Spring Boot 4.1.1 (test scope only, for the DataSource test; no web server is started)
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the pool statistics and logs for each step
mvn test                   # asserts every result shown in the article (18 tests)
```

## Files

| File | What it shows |
|---|---|
| ParkingEntry.java | Entity for a parking garage entry log (plate, enteredAt) |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and `hibernate.c3p0.*` settings, reads the c3p0 pool statistics |
| C3p0Demo.java | Built-in pool warning, c3p0 startup, pool growth, exhausted pool, idle shrinking, leak detection, database restart |
| C3p0PoolTest.java | JUnit tests for pool selection, settings, sizing, timeouts, leak detection and connection testing |
| SpringBootC3p0DataSourceTest.java | Spring Boot 4 with c3p0 as the DataSource (`spring.datasource.type` and a `@ConfigurationProperties` bean) |
