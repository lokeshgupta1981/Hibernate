# Hibernate count, min, max, sum, avg Functions

Source code for the article [Hibernate count, min, max, sum, avg Functions](https://howtodoinjava.com/hibernate/aggregate-functions/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # runs every query and prints the SQL and the result
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Runner.java | Marathon result: name, age group, city, finish time in minutes (null = did not finish), entry fee, club |
| Club.java | Running club with `@OneToMany` members, used for aggregates over a join |
| AgeGroupStats.java | Record that receives `group by` results (`select new` and plain record instantiation) |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and loads six runners and three clubs |
| AggregateFunctionsDemo.java | count, min, max, sum, avg, group by, having, nulls, joins, records, Criteria API, filter, listagg, percentiles, window functions |
| AggregateFunctionsTest.java | JUnit tests for every result and Java type described in the article |
