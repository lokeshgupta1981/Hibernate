# [SOLVED] java.lang.NoClassDefFoundError: org/hibernate/engine/SessionFactoryImplementor

Source code for the article [[SOLVED] java.lang.NoClassDefFoundError: org/hibernate/engine/SessionFactoryImplementor](https://howtodoinjava.com/hibernate/solved-java-lang-noclassdeffounderror-orghibernateenginesessionfactoryimplementor/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2), versions managed by the `hibernate-platform` BOM
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer
- Test scope only, to reproduce the error: Spring 3.2.18 (spring-orm, spring-jdbc, spring-tx, spring-beans, spring-core) without their transitive dependencies, plus javax.transaction-api 1.3 and commons-logging 1.2

## Run

```bash
mvn -q test-compile exec:java   # prints the error, the jar that causes it and the Hibernate 7 SPI calls
mvn test                        # asserts every error and result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Fixture.java | Entity for a sports fixture: home team, away team, kickoff, venue |
| Database.java | Bootstraps Hibernate 7 with `HibernatePersistenceConfiguration` and saves two fixtures |
| ClasspathScanner.java | Lists the classes in the jars on the class path that refer to a given class name |
| SessionFactoryImplementorDemo.java | Shows the error from Spring's `orm.hibernate3.HibernateTransactionManager`, the jar that holds the failing class, and `emf.unwrap(SessionFactoryImplementor.class)` |
| SessionFactoryImplementorErrorTest.java | JUnit tests: the package move, the `NoClassDefFoundError` with its stack frames, the jar lookup, and the SPI calls |
