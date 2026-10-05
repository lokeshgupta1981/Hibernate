# Hibernate get() vs load() Methods

Source code for the article [Hibernate get() vs load() Methods](https://howtodoinjava.com/hibernate/how-to-load-get-entity-in-hibernate/).

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
| Parcel.java | Parcel entity (`trackingCode`, `status`, `weightKg`) loaded by id |
| ScanEvent.java | A depot scan with a lazy `@ManyToOne` parcel (`parcel_id` foreign key) |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`, which returns a `SessionFactory` |
| SqlLog.java | `StatementInspector` that records each SQL statement for the tests |
| GetVsLoadDemo.java | Runs the deprecated `get()` and `byId()` calls next to `find()` and `getReference()` and prints the SQL |
| GetVsLoadTest.java | 23 JUnit tests for every behavior described in the article |
