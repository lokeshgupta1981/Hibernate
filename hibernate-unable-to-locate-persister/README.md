# Hibernate Error: Unable to locate persister

Source code for the article [Hibernate Error: Unable to locate persister](https://howtodoinjava.com/hibernate/unable-to-locate-persister-error/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2) with hibernate-scan-jandex
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Test scope only: Spring Boot 4.1.1 (spring-boot-starter-data-jpa) and javax.persistence-api 2.2
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # reproduces each cause, prints the error, then the fix with its SQL
mvn test                   # 20 tests: every error message and every fix shown in the article
```

The second Surefire execution runs `ScanningWithoutJandexTest` without hibernate-scan-jandex. Run the whole suite with `mvn test`; a `-Dtest=...` filter would also apply to that execution.

## Files

| File | What it shows |
|---|---|
| FerryRoute.java | The correct entity (`jakarta.persistence.Entity`) |
| missingentity/FerryRoute.java | Same class without `@Entity` |
| dto/FerryRouteDto.java | A record DTO with the same fields, passed by mistake to `persist()` and `find()` |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`, or from a `persistence.xml` unit |
| META-INF/persistence.xml | Units without `<class>`, with `<class>`, and with scanning (`exclude-unlisted-classes=false`) |
| PersisterErrorDemo.java | Runs every cause and fix and prints the messages and SQL |
| PersisterErrorTest.java | Asserts each error message and each fix (Hibernate only) |
| legacy/FerryRoute.java (test) | Entity annotated with the old `javax.persistence.Entity` |
| ScanningWithoutJandexTest.java | Runs in a second Surefire execution without hibernate-scan-jandex: the scanned unit finds no entity |
| SpringBootEntityScanTest.java | Spring Boot 4.1.1: entity outside the scanned packages, `Not a managed type` at startup, `@EntityScan` fix |
| boot/* (test) | The three small Spring Boot applications used by that test |
