# [SOLVED] Hibernate - No row with the given identifier exists

Source code for the article [[SOLVED] Hibernate - No row with the given identifier exists](https://howtodoinjava.com/hibernate/solved-org-hibernate-objectnotfoundexception-no-row-with-the-given-identifier-exists/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # reproduces each error and fix, and prints the SQL
mvn test                   # asserts every exception, message and result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Scholarship.java | The referenced entity (name, amount) |
| BaseApplication.java | Shared columns of the `scholarship_application` table (studentName, submittedOn) |
| ScholarshipApplication.java | Legacy mapping: `LAZY` `@ManyToOne` with `ConstraintMode.NO_CONSTRAINT` |
| EagerApplication.java | Same table with an `EAGER` `@ManyToOne` |
| IgnoreApplication.java | Same table with `@NotFound(action = NotFoundAction.IGNORE)` |
| ExceptionApplication.java | Same table with `@NotFound(action = NotFoundAction.EXCEPTION)` |
| CheckedApplication.java | Fixed mapping with a real foreign key constraint |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`; inserts the legacy rows with native SQL |
| NotFoundDemo.java | Runs each case and prints the SQL and the exception |
| NotFoundTest.java | 23 JUnit tests for every behavior described in the article |
