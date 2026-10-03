# [Solved] org.hibernate.QueryException: Cannot mix named and positional parameters

Source code for the article [[Solved] org.hibernate.QueryException: Cannot mix named and positional parameters](https://howtodoinjava.com/hibernate/solved-org-hibernate-queryexception-cannot-mix-named-and-positional-parameters/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # runs every case and prints the SQL or the exception
mvn test                   # asserts every result and error message shown in the article
```

## Files

| File | What it shows |
|---|---|
| TrainService.java | Entity: a train timetable entry (trainNumber, fromStation, toStation, departsAt) |
| Procedures.java | Java method that H2 exposes as the stored procedure `count_departures` |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`, loads 4 trains, creates the procedure; optional strict JPA query compliance |
| MixedParametersDemo.java | Runs each case (stored procedure, native SQL, HQL, colons in SQL, other parameter mistakes) and prints the SQL or the exception |
| MixedParametersTest.java | JUnit tests for every result and error message in the article |
