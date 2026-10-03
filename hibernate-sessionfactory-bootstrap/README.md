# Bootstrapping SessionFactory in Hibernate

Source code for the article [Bootstrapping SessionFactory in Hibernate](https://howtodoinjava.com/hibernate/hibarnate-build-sessionfactory/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # builds a SessionFactory in every way and prints the SQL
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Task.java | The entity of the task board: title, status, dueDate |
| SessionFactories.java | Every way to build a `SessionFactory`: `HibernatePersistenceConfiguration`, `StandardServiceRegistryBuilder` + `MetadataSources`, `Configuration`, `hibernate.properties`, `hibernate.cfg.xml`, and `unwrap()` from an `EntityManagerFactory` |
| TaskBoardDatabase.java | One `SessionFactory` per application, closed by a shutdown hook |
| SessionFactoryDemo.java | Builds each factory, saves and reads a task, uses one factory from five threads, closes it |
| hibernate.properties | Default settings that every bootstrap reads from the root of the classpath |
| hibernate.cfg.xml | Settings and mappings read by `configure()` |
| META-INF/persistence.xml | The `taskboard` persistence unit for `Persistence.createEntityManagerFactory()` |
| SessionFactoryBootstrapTest.java | JUnit tests for every behavior described in the article |
