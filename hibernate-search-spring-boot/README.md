# Hibernate Search Example with Lucene and Spring Boot

Source code for the article [Hibernate Search Example with Lucene and Spring Boot](https://howtodoinjava.com/hibernate/hibernate-search-with-lucene-and-spring-boot/).

A wine catalog with a full-text search REST endpoint: `GET /wines/search?q=...` with a region filter, sorting and paging.

## Versions

- Java 25
- Spring Boot 4.1.1 (spring-boot-starter-webmvc, spring-boot-starter-data-jpa)
- Hibernate ORM 7.4.11.Final (overrides the 7.4.5.Final managed by Spring Boot 4.1.1)
- Hibernate Search 8.4.0.Final with the Lucene backend (Lucene 9.12.3)
- H2 2.5.252 (in-memory database)
- Maven 3.9 or newer

## Run

```bash
mvn spring-boot:run   # starts on http://localhost:8080 and rebuilds the index from data.sql
mvn test              # 15 tests: @SpringBootTest + MockMvc for every result shown in the article
```

The Lucene index is written to `data/wine-index/Wine`. The console prints every SQL statement (`spring.jpa.show-sql=true`).

## Try it with curl

```bash
# Full-text search, best match first
curl "http://localhost:8080/wines/search?q=cherry"

# Stemming: "cherries" finds the same wines as "cherry"
curl "http://localhost:8080/wines/search?q=cherries"

# Filter by region (case-insensitive keyword field)
curl "http://localhost:8080/wines/search?q=plum&region=rioja"

# Sort by price, first page of 2
curl "http://localhost:8080/wines/search?q=cherry&sort=price_asc&size=2"

# All wines sorted by name, second page of 3
curl "http://localhost:8080/wines/search?sort=name&page=1&size=3"

# Save a new wine; it is searchable as soon as the transaction commits
curl -X POST http://localhost:8080/wines -H "Content-Type: application/json" \
  -d '{"name":"Beaujolais","region":"Burgundy","grape":"Gamay","tastingNotes":"Fresh red cherries and strawberry.","price":15.00}'
curl "http://localhost:8080/wines/search?q=gamay"
```

Sort values: `relevance` (default), `name`, `price_asc`, `price_desc`. Any other value returns HTTP 400.

## Files

| File | What it shows |
|---|---|
| pom.xml | Spring Boot 4.1.1 parent, Hibernate Search BOM, mapper and Lucene backend |
| application.properties | H2 datasource and the Hibernate Search settings with the `spring.jpa.properties.` prefix |
| data.sql | Eight seed wines (inserted with SQL, so not indexed until the MassIndexer runs) |
| Wine.java | `@Indexed` entity with `@FullTextField`, `@KeywordField` and `@GenericField` |
| WineAnalysisConfigurer.java | Spring bean that defines the `wine` analyzer and the `lowercase` normalizer |
| IndexRebuilder.java | Runs the MassIndexer on `ApplicationReadyEvent` (switch off with `wine.search.reindex-on-startup=false`) |
| WineSearchService.java | Search with `SearchSession`: match, region filter, sorting and paging |
| WineController.java | `GET /wines/search` and `POST /wines` |
| WinePage.java | JSON response: query, total, page, size, wines |
| WineSort.java | Accepted sort orders |
| WineRepository.java | Spring Data JPA repository used to save and delete wines |
| WineSearchTest.java | `@SpringBootTest` + MockMvc tests for searching, sorting, paging and automatic indexing |
| StartupWithoutIndexingTest.java | Shows that data.sql rows are not searchable without the MassIndexer, and that unprefixed properties are ignored |
