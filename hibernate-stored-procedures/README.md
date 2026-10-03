# Hibernate Stored Procedures

Source code for the article [Hibernate Stored Procedures: Call with StoredProcedureQuery](https://howtodoinjava.com/hibernate/stored-procedures/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- MySQL 9 (started in Docker by Testcontainers 2.0.5), MySQL Connector/J 26.7.0
- Spring Data JPA 4.1.1 (only for the `@Procedure` repository example)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

Docker must be running, because Testcontainers starts a MySQL container.

```bash
mvn -q compile exec:java   # prints the procedure calls and their results
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| procedures.sql | The three MySQL procedures: `count_courses_by_level`, `find_courses_by_level`, `apply_discount` |
| Course.java | Entity mapped to the `course` table, with a `@NamedStoredProcedureQuery` |
| CourseProcedures.java | Every way to call the procedures: OUT parameters, result sets, positional parameters, updates, named procedure, Hibernate `ProcedureCall` |
| CourseRepository.java | Spring Data JPA repository method with `@Procedure` |
| Database.java | Starts MySQL with Testcontainers, bootstraps Hibernate and creates the procedures |
| StoredProcedureDemo.java | Runs each call and prints the result |
| StoredProcedureTest.java | JUnit tests for every result shown in the article |
