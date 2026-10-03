# Hibernate.initialize(): Load a Proxy or a Lazy Collection

Source code for the article [Hibernate.initialize(): Load a Proxy or a Lazy Collection](https://howtodoinjava.com/hibernate/use-hibernate-initialize-to-initialize-proxycollection/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL and the result of each step
mvn test                   # asserts every result and statement count shown in the article
```

## Files

| File | What it shows |
|---|---|
| Team.java | Parent entity with a lazy `@OneToMany` players collection |
| Player.java | Child entity with a lazy `@ManyToOne` team (a proxy until initialized) |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`, seeds three teams, counts statements with Hibernate statistics; variants with `hibernate.default_batch_fetch_size` and `hibernate.enable_lazy_load_no_trans` |
| InitializeDemo.java | Runs `Hibernate.initialize()`, `isInitialized()`, `unproxy()`, `size()`, `contains()`, `isEmpty()`, `PersistenceUnitUtil`, the closed-session errors and the N+1 comparison, printing the SQL |
| InitializeTest.java | 22 JUnit tests for every behavior and statement count in the article |
