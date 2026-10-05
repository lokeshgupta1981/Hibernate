# Handling BLOB and CLOB with Hibernate

Source code for the article [Handling BLOB and CLOB with Hibernate](https://howtodoinjava.com/hibernate/hibernate-example-of-insertselect-blob-from-database/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q process-classes exec:java   # prints the DDL and the SQL for each step
mvn test                           # asserts every result shown in the article
```

`process-classes` (or any later phase) is needed because `hibernate-maven-plugin` enhances `ApplicationFile` after compilation.

## Files

| File | What it shows |
|---|---|
| JobApplication.java | `@Lob byte[]` resume and `@Lob String` cover letter; `@Basic(fetch = LAZY)` without enhancement |
| ApplicationFile.java | `@Lob java.sql.Blob` and `java.sql.Clob`, lazy through bytecode enhancement |
| JobPosting.java | Large columns without `@Lob`: `@JdbcTypeCode(SqlTypes.LONG32VARCHAR)` and `@Column(length = ...)` |
| ApplicationDraft.java | `byte[]` and `String` without `@Lob` (varbinary(255) / varchar(255)) |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`; prints MySQL and PostgreSQL DDL without a connection |
| Samples.java | Creates the 20 MB test file and measures the heap held by a loaded `byte[]` and `Blob` |
| LobMappingDemo.java | Runs each step and prints the SQL |
| LobMappingTest.java | JUnit tests for every behavior described in the article |
| files/lokesh-resume.pdf | The 778-byte sample resume |
