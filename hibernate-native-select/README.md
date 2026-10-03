# Hibernate / JPA @NamedNativeQuery Example

Source code for the article [Hibernate / JPA @NamedNativeQuery Example](https://howtodoinjava.com/jpa/jpa-native-query-example-select/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL and the result of each native query
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Produce.java | Entity with the `@NamedNativeQuery` declarations (entity result, `@ConstructorResult` DTO, two-entity join) and their `@SqlResultSetMapping`s |
| Farm.java | Entity on the `@ManyToOne` side, loaded together with `Produce` through `@EntityResult` |
| ProducePrice.java | Record filled from `createNativeQuery(sql, ProducePrice.class)` and a `TupleTransformer` |
| CategoryPrice.java | Record filled through `@ConstructorResult` |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) and inserts two farms and six kinds of produce |
| NativeSelectDemo.java | Runs each native query and prints the SQL and the result |
| NativeSelectTest.java | 31 JUnit tests for every result, Java type and error message in the article |
| StartupCheckEntities.java | Test-only entities: a named native query and a JPQL named query with a wrong table name, to show which one fails at startup |
