# Hibernate/JPA Persistence Annotations

Source code for the article [Hibernate/JPA Persistence Annotations](https://howtodoinjava.com/hibernate/hibernate-jpa-2-persistence-annotations-tutorial/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the generated DDL and the SQL for each step
mvn test                   # asserts every DDL line and result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Theater.java | Default table name, `IDENTITY` key, `columnDefinition`, `@Embedded` with `@AttributeOverride` |
| Address.java | `@Embeddable` class |
| BoxOffice.java | `@Embeddable` record (Jakarta Persistence 3.2) |
| Play.java | `@Table` with index and comment, `SEQUENCE` key, `@Column` attributes and `check`, `@Enumerated(STRING)`, `@Lob`, `@Basic(fetch = LAZY)`, `@Convert`, `@Version`, `@Access(PROPERTY)`, `@Transient` |
| Performance.java | `@Entity(name = "Show")`, `@Table` unique constraint and index, `UUID` key, `ORDINAL` enum, `@EnumeratedValue` enum, read-only `insertable = false, updatable = false` column |
| Genre.java, Status.java, Language.java | Enums; `Language` uses `@EnumeratedValue` |
| YesNoConverter.java | `AttributeConverter` that stores a boolean as 'Y' / 'N' |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and writes the DDL script |
| AnnotationsDemo.java | Prints the DDL and the SQL for each step |
| AnnotationsTest.java | JUnit tests for every DDL line, value and error in the article |
| DuplicateColumnShow.java (test) | Maps `play_id` twice to reproduce the duplicated column error |
