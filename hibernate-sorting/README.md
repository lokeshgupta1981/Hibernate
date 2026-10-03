# Hibernate ORDER BY and JPA Sorting: HQL, Criteria, @OrderBy

Source code for the article [Hibernate ORDER BY and JPA Sorting: HQL, Criteria, @OrderBy](https://howtodoinjava.com/hibernate/hibernate-jpa-sorting/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL and the result of every sorting query
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Trail.java | Entity with `@OrderBy`, `@SortComparator`, `@SQLOrder`, `@OrderColumn` and `@SortNatural` collections |
| TrailReview.java | Review entity (author, rating) with the `trail_id` foreign key |
| ReviewByRating.java | `Comparator` used by `@SortComparator`: highest rating first, then author |
| TrailQueries.java | JPQL `order by`, Criteria API `orderBy` with `Nulls`, `lower()`, the allow-list dynamic sort and Hibernate 7 `SelectionSpecification.sort()` |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and loads the sample trails |
| SortingDemo.java | Runs each query and prints the SQL and the result |
| SortingTest.java | 28 JUnit tests for every result described in the article |
