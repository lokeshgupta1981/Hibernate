# [Solved] HsqlException: data exception: invalid character value for cast

Source code for the article [[Solved] HsqlException: data exception: invalid character value for cast](https://howtodoinjava.com/hibernate/solved-hsqlexception-data-exception-invalid-character-value-for-cast/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- HSQLDB 2.7.4 (in-memory database)
- H2 2.5.252 (only to compare the error messages)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # reproduces each cause and fix and prints the exception chain
mvn test                   # asserts every error message and fix shown in the article
```

## Files

| File | What it shows |
|---|---|
| BusPass.java | Entity with a numeric `zone`, a `LocalDate validUntil` and a `PassType` enum stored with `EnumType.STRING` |
| OrdinalBusPass.java | The same table mapped with the default `ORDINAL` enum mapping, to show the reverse mismatch |
| PassType.java | The enum `MONTHLY`, `WEEKLY`, `STUDENT` |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` for HSQLDB or H2, and runs plain SQL with JDBC |
| RootCause.java | Finds the `SQLException` in a cause chain |
| CastErrorDemo.java | Runs each cause (String for a number, enum mapping, date format, column order in INSERT and in a stored procedure) and its fix |
| CastErrorTest.java | 16 JUnit tests for every error and fix, including the H2 messages for the same mistakes |
