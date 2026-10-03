# Hibernate Hello World Example

Source code for the article [Hibernate Hello World Example](https://howtodoinjava.com/hibernate/hibernate-hello-world-application/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # saves, reads, updates and deletes a note and prints the SQL
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Note.java | The entity: `@Entity`, `@Id`, `@GeneratedValue`, `@Column` |
| Database.java | Three ways to start Hibernate: `HibernatePersistenceConfiguration` (Java), `persistence.xml`, and the legacy `hibernate.cfg.xml` |
| HelloWorldDemo.java | Save, read, query, update, merge and delete a note, then the same with the `Session` API |
| src/main/resources/META-INF/persistence.xml | Jakarta Persistence configuration file |
| src/main/resources/hibernate.cfg.xml | Hibernate native configuration file (still supported in 7.4) |
| src/test/resources/hibernate.properties | Settings read automatically from the classpath root |
| HelloWorldTest.java | 23 JUnit tests for every behavior described in the article |
