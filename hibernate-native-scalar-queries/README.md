# Pure Native Scalar Queries Are Not Yet Supported: Fix

Source code for the article [Pure Native Scalar Queries Are Not Yet Supported: Fix](https://howtodoinjava.com/hibernate/workaround-jpa-hibernate-notyetimplementedexception-pure-native-scalar-queries-are-not-yet-supported/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

The folder `legacy-hibernate-4.1` is a separate Maven project that reproduces the old error with
hibernate-entitymanager 4.1.12.Final and H2 1.4.200 on Java 25. Never use those versions in a real project.

## Run

```bash
mvn -q compile exec:java   # runs every scalar native query on Hibernate 7.4 and prints the SQL and Java types
mvn test                   # asserts every result shown in the article

cd legacy-hibernate-4.1
mvn -q compile exec:java   # prints the NotYetImplementedException and runs the @ColumnResult workaround
mvn test                   # asserts the error and the workaround on Hibernate 4.1.12
mvn test -Dhibernate.version=4.2.0.Final   # the two error tests fail: 4.2.0 no longer throws
```

## Files

| File | What it shows |
|---|---|
| Sighting.java | Entity with named native queries without `resultClass`, with a basic `resultClass`, and with a `@SqlResultSetMapping` using `@ColumnResult` |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) and adds four sightings |
| NativeScalarDemo.java | Runs each scalar query and prints the SQL, the values and their Java types |
| NativeScalarTest.java | JUnit tests for every result in the article |
| legacy-hibernate-4.1/.../broken/Sighting.java | Scalar `@NamedNativeQuery` without a mapping: fails at startup on Hibernate 3.6 to 4.1 |
| legacy-hibernate-4.1/.../brokenupdate/Sighting.java | Native UPDATE as a named query without a mapping: fails the same way |
| legacy-hibernate-4.1/.../fixed/Sighting.java | The workaround: `resultSetMapping` with `@ColumnResult` |
| legacy-hibernate-4.1/.../LegacyDemo.java, LegacyTest.java | Reproduce the error and test the workaround |
