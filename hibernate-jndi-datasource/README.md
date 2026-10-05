# Mocking an In-memory JNDI DataSource

Source code for the article [Mocking an In-memory JNDI DataSource](https://howtodoinjava.com/hibernate/mock-jndi-datasource/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- Simple-JNDI 0.25.0 (test scope, only for `SimpleJndiTest`)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # binds an H2 DataSource in JNDI, bootstraps Hibernate with the JNDI name, prints the SQL
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| City.java | The entity: name, country, population |
| InMemoryContextFactory.java | A JNDI provider in one small class, no dependency: one shared map behind `InitialContext` |
| Database.java | `fromJndi()` bootstraps Hibernate with `nonJtaDataSource("java:comp/env/jdbc/cities")`; `fromDataSource()` passes the `DataSource` object without JNDI; `h2()` creates the H2 `DataSource` |
| JndiDemo.java | Runs each step and prints the SQL and the errors |
| InMemoryJndiTest.java | Binds the H2 `DataSource` with the in-memory provider and runs CRUD, plus the error cases |
| SimpleJndiTest.java | The same lookup with Simple-JNDI, configured by `src/test/resources/jndi.properties` and `jndi/jdbc.properties` |
| DirectDataSourceTest.java | Hibernate with the `DataSource` object, no JNDI provider at all |
