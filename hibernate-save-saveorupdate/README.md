# Hibernate save(), update() and saveOrUpdate()

Source code for the article [Hibernate save(), update() and saveOrUpdate()](https://howtodoinjava.com/hibernate/hibernate-save-and-saveorupdate/).

`Session.save()`, `update()` and `saveOrUpdate()` were deprecated in Hibernate 6.0 and removed in Hibernate 7.0. This folder holds two Maven projects:

- the main project runs the Hibernate 7 replacements `persist()` and `merge()`;
- `hibernate6-legacy/` runs the old methods on Hibernate 6.6, the last version that has them.

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2); Hibernate ORM 6.6.58.Final in `hibernate6-legacy/`
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # persist() and merge() for each entity state, with the SQL at the call and at commit
mvn test                   # asserts every Hibernate 7 result shown in the article

cd hibernate6-legacy
mvn -q compile exec:java   # save(), update() and saveOrUpdate() on Hibernate 6.6
mvn test                   # asserts every Hibernate 6.6 result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Plant.java | Plant entity (`name`, `species`, `potSize`, `price`) with an `IDENTITY` id |
| PlantStore.java | A `saveOrUpdate()` replacement: `persist()` for a new plant, `merge()` otherwise, like Spring Data `save()` |
| Database.java | Bootstraps Hibernate 7 with `HibernatePersistenceConfiguration` (no persistence.xml) |
| SqlLog.java | `StatementInspector` that records each SQL statement for the demo and the tests |
| SaveOrUpdateDemo.java | Runs `persist()` and `merge()` on a new, managed, detached and missing plant and prints the SQL |
| PersistMergeTest.java | 13 JUnit tests for the Hibernate 7 behavior |
| hibernate6-legacy/.../LegacyDemo.java | Runs `save()`, `update()` and `saveOrUpdate()` on Hibernate 6.6 and prints the SQL |
| hibernate6-legacy/.../LegacyMethodsTest.java | 15 JUnit tests for the Hibernate 6.6 behavior |
