# Hibernate @Immutable Entities

Source code for the article [Hibernate @Immutable Entities](https://howtodoinjava.com/hibernate/immutable-annotation/).

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
| LedgerEntry.java | `@Immutable` entity: changes are ignored, `persist()` and `remove()` still work |
| Journal.java | Mutable entity with an `@Immutable` collection, an `@Immutable` attribute and a `@Column(updatable = false)` attribute |
| Memo.java, MemoConverter.java | Mutable value class stored through an `@Immutable` `AttributeConverter` |
| JournalBalance.java | Read-only entity mapped to a SQL query with `@Subselect` and `@Synchronize` |
| EntryLine.java | Java record used as a read-only query result |
| SqlLog.java | `StatementInspector` that records the SQL so the tests can check for `UPDATE` statements |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) and sets `hibernate.query.immutable_entity_update_query_handling_mode` |
| ImmutableDemo.java | Runs each step and prints the SQL |
| ImmutableEntityTest.java | JUnit tests for every behavior described in the article |
| LedgerEntryRecord.java (test) | A record mapped as an entity, to show the error Hibernate throws when loading it |
