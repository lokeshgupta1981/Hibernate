# Hibernate Entity Lifecycle Events and Callbacks

Source code for the article [Hibernate Entity Lifecycle Events and Callbacks](https://howtodoinjava.com/hibernate/entity-lifecycle-events-callbacks/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints every callback between the SQL statements
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| ExpenseClaim.java | Entity with all seven callback methods (`@PrePersist` ... `@PostLoad`), IDENTITY id, `@EntityListeners` |
| TravelClaim.java | SEQUENCE id, `@CreationTimestamp` / `@UpdateTimestamp`, `@ExcludeDefaultListeners` |
| ClaimHistory.java | Entity that a callback writes in the "what not to do" examples |
| ClaimValidationListener.java | Entity listener: rejects a claim without a positive amount (`@PrePersist`, `@PreUpdate`) |
| ClaimNotificationListener.java | Entity listener: `@PostPersist` and `@PostUpdate` notifications |
| AuditListener.java | Default listener for every entity, registered in `META-INF/orm.xml` |
| RiskyListener.java | Callbacks that run a query, call `persist()` or change the entity at the wrong time (default listener in `META-INF/orm-risky.xml`, used only by `Database.createRisky()`) |
| ClaimCommitListener.java | Hibernate `PostCommitInsertEventListener` that runs after commit |
| ClaimEventsIntegrator.java | Registers the Hibernate listener through `EventListenerRegistry` (found via `META-INF/services`) |
| CallbackLog.java, SqlRecorder.java | Record callbacks and SQL statements in the order they run |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and `META-INF/orm.xml` (default listener `AuditListener`) |
| CallbacksDemo.java | Runs each case and prints the callbacks between the SQL |
| EntityCallbacksTest.java | JUnit tests for every behavior described in the article |
