# Hibernate One-to-One Mapping with JPA @OneToOne Examples

Source code for the article [Hibernate One-to-One Mapping with JPA @OneToOne Examples](https://howtodoinjava.com/hibernate/hibernate-one-to-one-mapping/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the DDL and SQL for each mapping variant
mvn test                   # asserts every result shown in the article
```

Each variant maps the same two tables (`booking`, `boarding_pass`) and runs against its own in-memory database.

## Files

| File | What it shows |
|---|---|
| foreignkey/Booking.java, foreignkey/BoardingPass.java | `@OneToOne` + `@JoinColumn` on the boarding pass, `mappedBy` on the booking, `LAZY`, cascade, `orphanRemoval`, `optional = false` |
| eager/Booking.java, eager/BoardingPass.java | The same mapping with the JPA defaults (`EAGER`, optional) |
| sharedkey/Booking.java, sharedkey/BoardingPass.java | Shared primary key with `@MapsId` |
| jointable/Booking.java, jointable/BoardingPass.java | Link stored in a `@JoinTable` |
| primarykey/Booking.java, primarykey/BoardingPass.java | The older `@PrimaryKeyJoinColumn` mapping, for comparison |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) and counts SQL statements |
| OneToOneDemo.java | Runs each case and prints the SQL |
| OneToOneMappingTest.java | JUnit tests for every behavior described in the article |
