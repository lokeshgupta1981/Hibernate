# JPA Native UPDATE Query: @NamedNativeQuery Example

Source code for the article [JPA Native UPDATE Query: @NamedNativeQuery Example](https://howtodoinjava.com/hibernate/jpa-native-update-sql-query-example/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- Second-level cache: hibernate-jcache 7.4.11.Final with Caffeine JCache 3.3.0
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL and the result of each case
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Plan.java | Cached entity with two `@NamedNativeQuery` UPDATE statements (one with the `org.hibernate.query.native.spaces` hint) and a `@PreUpdate` counter |
| Addon.java | Second cached entity in its own table, used to see which cache regions a native update evicts |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`, enables the second-level and query cache, seeds and resets the data |
| NativeUpdateDemo.java | Runs each case and prints the SQL |
| NativeUpdateTest.java | 27 JUnit tests for every behavior described in the article |
