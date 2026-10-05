# Guide to Hibernate Search

Source code for the article [Guide to Hibernate Search](https://howtodoinjava.com/hibernate/hibernate-search/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- Hibernate Search 8.4.0.Final with the Lucene backend (Apache Lucene 9.12.3)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

No search server is needed: the Lucene index is kept in memory (`hibernate.search.backend.directory.type=local-heap`).

## Run

```bash
mvn -q compile exec:java   # prints every search result and the SQL Hibernate runs
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Episode.java | Indexed entity: `@Indexed`, `@FullTextField`, `@KeywordField`, `@GenericField`, sortable and highlightable fields |
| EnglishAnalysisConfigurer.java | Custom `english` analyzer (lowercase + English stemming) and `lowercase` normalizer |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and the Hibernate Search properties; sample episodes |
| EpisodeSearch.java | Every search from the article: match, boost, fuzzy, phrase, bool, range, sort, pagination, highlighting, count |
| HibernateSearchDemo.java | Runs each step and prints the results and the SQL |
| HibernateSearchTest.java | 17 JUnit tests for every behavior described in the article |
