# Inserting Objects with Hibernate

Source code for the article [Inserting Objects with Hibernate](https://howtodoinjava.com/hibernate/hibernate-insert-query-tutorial/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL for each step
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Pet.java | Entity with a `SEQUENCE` id: the id is set at `persist()`, the INSERT runs at flush |
| Adopter.java | Entity with an `IDENTITY` id: the INSERT runs at `persist()` |
| Kennel.java | Entity with an assigned id (kennel number), used for the duplicate id errors |
| AdoptedPet.java | Target of the HQL `insert ... select` statement |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) |
| SqlLog.java | `StatementInspector` that records the SQL so the tests can check when it runs |
| InsertDemo.java | Runs each case and prints the SQL |
| InsertTest.java | JUnit tests for every behavior described in the article |
