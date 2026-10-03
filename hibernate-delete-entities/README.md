# Deleting Entities with Hibernate

Source code for the article [Deleting Entities with Hibernate](https://howtodoinjava.com/hibernate/deleting-entities/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL for each delete scenario
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Newsletter.java | Parent entity with a `@OneToMany` subscribers list and no cascade |
| Subscriber.java | Child entity that owns the `newsletter_id` foreign key and has a `@PreRemove` callback |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`, seeds the data and reads statement statistics |
| DeleteEntitiesDemo.java | Runs each scenario on a fresh database and prints the SQL: `find()` + `remove()`, `getReference()` + `remove()`, detached entity, bulk delete, stale objects, foreign key errors, children first |
| DeleteEntitiesTest.java | JUnit tests for every behavior described in the article |
