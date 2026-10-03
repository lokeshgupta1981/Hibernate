# Guide to Hibernate Query Language (HQL)

Source code for the article [Guide to Hibernate Query Language (HQL)](https://howtodoinjava.com/hibernate/complete-hibernate-query-language-hql-tutorial/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # runs every query and prints the SQL Hibernate generates
mvn test                   # asserts every result and SQL statement shown in the article
```

## Files

| File | What it shows |
|---|---|
| Gallery.java | Entity mapped to the `galleries` table, `@OneToMany` artworks |
| Artist.java | Entity mapped to the `artists` table, `@OneToMany` artworks |
| Artwork.java | Entity mapped to the `artworks` table: `year` attribute in the `created_year` column, `Medium` enum, `@ManyToOne` artist and gallery |
| Medium.java | Enum stored as a string |
| Highlight.java | Target entity of the `insert ... select` example |
| ArtworkSummary.java | Record used with `select new` and with implicit instantiation |
| SqlLog.java | `StatementInspector` that records the SQL so the tests can check it |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and inserts the sample data |
| HqlDemo.java | Runs each query and prints its SQL and result |
| HqlTest.java | 40 JUnit tests for every result in the article |
