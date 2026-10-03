# Hibernate Merging and Refreshing Entities

Source code for the article [Hibernate Merging and Refreshing Entities](https://howtodoinjava.com/hibernate/merging-and-refreshing-hibernate-entities/).

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
| Shipment.java | Entity with `destination`, `status` and a `@Version` column |
| ReceivedTrigger.java | H2 trigger that sets `status = 'received'` on insert, read back with `refresh()` |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) and creates the trigger |
| MergeRefreshDemo.java | Runs each `merge()` and `refresh()` case and prints the SQL |
| MergeRefreshTest.java | JUnit tests for every behavior described in the article |
