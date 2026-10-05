# Errors in Named Queries

Source code for the article [[Solved] Initial SessionFactory creation failed.org.hibernate.HibernateException: Errors in named queries](https://howtodoinjava.com/hibernate/solved-initial-sessionfactory-creation-failed-org-hibernate-hibernateexception-errors-in-named-queries/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # runs the fixed queries, then prints the startup error of each broken query
mvn test                   # asserts every error message and every fix shown in the article
```

## Files

| File | What it shows |
|---|---|
| Volunteer.java | Entity with name and phone |
| Shift.java | Entity mapped to the `volunteer_shift` table, with the fixed `@NamedQuery` and `@NamedNativeQuery` declarations |
| BrokenQueries.java | One nested class per broken named query (table name, column name, typo, unregistered entity, SQL in JPQL, keyword typo, semicolon, `?` and `?0` parameters, wrong literal type, two errors, broken native query) |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and adds the classes under test |
| HibernateUtil.java | The old static `HibernateUtil` helper that prints "Initial SessionFactory creation failed." |
| NamedQueryErrorsDemo.java | Runs the fixed queries with SQL logging and prints each startup error |
| NamedQueryErrorsTest.java | 17 JUnit tests: each error message, `getErrors()`, `hibernate.query.startup_check = false`, native queries at runtime, and every fix |
