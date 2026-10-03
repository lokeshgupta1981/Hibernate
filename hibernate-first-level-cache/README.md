# Guide to Hibernate First Level Cache

Source code for the article [Guide to Hibernate First Level Cache](https://howtodoinjava.com/hibernate/understanding-hibernate-first-level-cache-with-example/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL and the result of each step
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Movie.java | The entity used in every example |
| SqlCounter.java | A `StatementInspector` that records each SQL statement, so the examples can count them |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml), registers `SqlCounter` and turns on statistics |
| FirstLevelCacheDemo.java | Runs each case (repeated `find()`, two EntityManagers, queries, `refresh()`, `detach()`, `evict()`, `clear()`, batch inserts, `StatelessSession`) and prints the SQL |
| FirstLevelCacheTest.java | JUnit tests for every behavior described in the article |
