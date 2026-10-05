# Hibernate Entity LifeCycle

Source code for the article [Hibernate Entity LifeCycle](https://howtodoinjava.com/hibernate/hibernate-entity-persistence-lifecycle-states/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # moves one ServiceOrder through every state and prints the SQL
mvn test                   # asserts every state, statement and exception shown in the article
```

## Files

| File | What it shows |
|---|---|
| ServiceOrder.java | The entity: plate number, issue, status and cost, with a sequence-generated id |
| EntityState.java | The four states: TRANSIENT, MANAGED, DETACHED, REMOVED |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml), records the SQL, and `stateOf()` works out the state of an object |
| EntityStatesDemo.java | Runs persist, dirty checking, close, merge, find, query, detach, clear, remove, persist after remove, the error cases and flush, and prints the SQL |
| EntityStatesTest.java | 24 JUnit tests for every behavior described in the article |
