# EntityManager getReference() vs find()

Source code for the article [EntityManager getReference() vs find()](https://howtodoinjava.com/hibernate/get-reference-vs-find/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL for each step
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Agent.java | Support agent entity, the target of `find()` and `getReference()` |
| Ticket.java | Support ticket with a lazy `@ManyToOne` assignee (`assignee_id` foreign key) |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and enables statistics to count SQL statements |
| FindVsGetReferenceDemo.java | Runs each case (existing row, missing row, assigning a ticket, closed EntityManager, JPA 3.2 options) and prints the SQL |
| FindVsGetReferenceTest.java | 23 JUnit tests for every behavior described in the article |
