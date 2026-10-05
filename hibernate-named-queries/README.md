# Hibernate Named Query Examples

Source code for the article [Hibernate Named Query Examples](https://howtodoinjava.com/hibernate/hibernate-named-query-tutorial/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- Hibernate Processor 7.4.11.Final (generates `Room_`, `Booking_` and `HotelQueries_` at compile time)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL and the result of each named query
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Room.java | `@NamedQuery` (named, positional and collection parameters, scalar and two-column results, update) and `@NamedNativeQuery` with `resultClass` and `@SqlResultSetMapping` |
| Booking.java | `@NamedQuery` with `join fetch`, and Hibernate's `@org.hibernate.annotations.NamedQuery` with `readOnly`, `timeout` and `fetchSize` |
| RoomRate.java, RoomCount.java | Records used as query result types |
| HotelQueries.java | `@HQL` query methods, checked at compile time by Hibernate Processor |
| BrokenQueries.java | A named query with a typo; registering it makes the startup fail |
| UncheckedNativeQueries.java | A native named query with a wrong table name; it fails only when it runs |
| src/main/resources/META-INF/orm.xml | Named queries declared in XML |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) and saves the sample rooms and bookings |
| NamedQueryDemo.java | Runs each query and prints the SQL and the result |
| NamedQueryTest.java | JUnit tests for every result and error described in the article |
| DuplicateQueries.java, BadEntityNameQueries.java, BadTypeQueries.java (test) | A duplicate query name, a wrong entity name and a type mismatch, each failing at startup |
| override-orm.xml (test) | An XML query that replaces the annotation with the same name |
