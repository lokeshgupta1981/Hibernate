# Configure In-memory DB to Unit Test Hibernate

Source code for the article [Configure In-memory DB to Unit Test Hibernate](https://howtodoinjava.com/hibernate/hibernate-in-memory-database-with-junit/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database, test scope)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn test                        # 18 JUnit tests: the repository tests and the H2 behavior checks
mvn -q test-compile exec:java   # prints the SQL of each lifecycle step and a timing comparison
```

The demo runs with the test classpath (`classpathScope=test`), because H2 and `import.sql` are test resources.

## Files

| File | What it shows |
|---|---|
| Invoice.java | Entity: client, amount, issuedOn, paid |
| InvoiceRepository.java | Repository-style class that takes an `EntityManager` |
| Database.java | `HibernatePersistenceConfiguration` for an in-memory H2 database (`MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1`, `create-drop`) |
| InvoiceDemo.java | Runs the lifecycle steps (`@BeforeAll`, `@BeforeEach`, `@Test`, `@AfterEach`, `@AfterAll`) and prints the SQL |
| src/test/resources/import.sql | Three invoices loaded after the schema is created and after every `truncate()` |
| InvoiceRepositoryTest.java | One factory per class, `truncate()` before every test, a rolled back transaction per test |
| H2BehaviorTest.java | `DB_CLOSE_DELAY`, `DATABASE_TO_LOWER`, identity restart, load script settings, multi-line scripts, a MySQL function that H2 lacks |
| src/test/resources/*.sql | Extra load scripts used by `H2BehaviorTest` |
