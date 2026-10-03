# [SOLVED] Bean property 'configurationClass' is not writable or has an invalid setter method

Source code for the article [[SOLVED] Bean property 'configurationClass' is not writable or has an invalid setter method](https://howtodoinjava.com/hibernate/solved-bean-property-configurationclass-is-not-writable-or-has-an-invalid-setter-method/).

## Versions

- Java 25
- Spring Boot 4.1.1 (Spring Framework 7.0.9, spring-boot-starter-data-jpa)
- Hibernate ORM 7.4.11.Final (overrides the 7.4.5.Final managed by Spring Boot 4.1.1)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # 1. the error, 2. the XML fix, 3. the Spring Boot setup, with the SQL
mvn test                   # 12 tests for every error message and result shown in the article
```

## Files

| File | What it shows |
|---|---|
| legacy/session-factory-broken.xml | `LocalSessionFactoryBean` with `configurationClass`: throws `NotWritablePropertyException` |
| legacy/session-factory-packages.xml | Fix: no `configurationClass`, entities found with `packagesToScan` |
| legacy/session-factory-annotated-classes.xml | Fix: no `configurationClass`, entities listed with `annotatedClasses` |
| legacy/session-factory-hibernate5.xml | Spring 5/6 class name `orm.hibernate5`: throws `CannotLoadBeanClassException` on Spring 7 |
| application.properties | Spring Boot data source and JPA settings (no SessionFactory bean) |
| Showtime.java | The entity: movie title, screen, start time |
| ShowtimeApplication.java | Spring Boot application class |
| ShowtimeRepository.java | Spring Data JPA repository |
| ShowtimeDao.java | Native Hibernate `Session` from the transactional `EntityManager` |
| ConfigurationClassDemo.java | Runs each step and prints the error or the SQL |
| ConfigurationClassTest.java | JUnit tests for the errors, both XML fixes, plain Hibernate `Configuration` and the Spring Boot setup |
